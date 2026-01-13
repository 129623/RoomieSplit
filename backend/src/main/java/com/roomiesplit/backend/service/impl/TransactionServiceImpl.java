package com.roomiesplit.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.LedgerMember;
import com.roomiesplit.backend.domain.TransactionParticipant;
import com.roomiesplit.backend.domain.TransactionRecord;
import com.roomiesplit.backend.dto.CreateTransactionRequest;
import com.roomiesplit.backend.mapper.LedgerMemberMapper;
import com.roomiesplit.backend.mapper.TransactionParticipantMapper;
import com.roomiesplit.backend.mapper.TransactionRecordMapper;
import com.roomiesplit.backend.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 交易服务实现类
 */
@Service
public class TransactionServiceImpl extends ServiceImpl<TransactionRecordMapper, TransactionRecord>
        implements TransactionService {

    @Autowired
    private TransactionParticipantMapper participantMapper;

    @Autowired
    private LedgerMemberMapper ledgerMemberMapper;

    /**
     * 创建一笔新交易
     *
     * @param request 交易请求对象，包含金额、分类、日期及参与人及其分摊详情
     * @param userId  记录本次交易的用户ID
     * @return 包含新交易记录的 Result 对象
     */
    @Override
    @Transactional
    public Result<?> createTransaction(CreateTransactionRequest request, Long userId) {
        // 1. 保存交易主记录
        TransactionRecord record = new TransactionRecord();
        record.setLedgerId(request.getLedgerId());
        // 如果请求未指定支付者，默认记录者为支付者 (垫付人)
        record.setPayerId(request.getPayerId() != null ? request.getPayerId() : userId);
        record.setAmount(request.getAmount());
        record.setCurrency("CNY");
        record.setCategory(request.getCategory());
        record.setDescription(request.getDescription());
        record.setTransactionDate(request.getDate() != null ? request.getDate() : LocalDateTime.now());
        record.setSplitType(request.getSplitType() != null ? request.getSplitType() : "EQUAL");
        record.setCreatedAt(LocalDateTime.now());

        this.save(record);

        // 2. 处理分摊逻辑
        List<CreateTransactionRequest.ReqParticipant> reqParticipants = request.getParticipants();

        // 默认逻辑补全：如果未指定参与者且分摊类型为 EQUAL，默认所有成员参与分摊
        if ((reqParticipants == null || reqParticipants.isEmpty()) && "EQUAL".equalsIgnoreCase(record.getSplitType())) {
            LambdaQueryWrapper<LedgerMember> memberQuery = new LambdaQueryWrapper<>();
            memberQuery.eq(LedgerMember::getLedgerId, request.getLedgerId());
            List<LedgerMember> members = ledgerMemberMapper.selectList(memberQuery);

            reqParticipants = new ArrayList<>();
            for (LedgerMember m : members) {
                CreateTransactionRequest.ReqParticipant p = new CreateTransactionRequest.ReqParticipant();
                p.setUserId(m.getUserId());
                reqParticipants.add(p);
            }
        }

        if (reqParticipants == null || reqParticipants.isEmpty()) {
            return Result.error(400, "必须指定参与者");
        }

        BigDecimal totalAmount = request.getAmount();
        int count = reqParticipants.size();

        // 预先计算人均分摊金额 (仅针对 EQUAL 模式)
        BigDecimal splitAmountPerPerson = BigDecimal.ZERO;
        if ("EQUAL".equalsIgnoreCase(record.getSplitType())) {
            if (count > 0) {
                splitAmountPerPerson = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal accumulated = BigDecimal.ZERO;

        for (int i = 0; i < count; i++) {
            CreateTransactionRequest.ReqParticipant rp = reqParticipants.get(i);
            TransactionParticipant tp = new TransactionParticipant();
            tp.setTransactionId(record.getId());
            tp.setUserId(rp.getUserId());

            // 计算该参与者的应付金额 (Owing Amount)
            if ("EQUAL".equalsIgnoreCase(record.getSplitType())) {
                if (i == count - 1) {
                    // 最后一个人承担剩余部分，确保总金额无精度损失
                    tp.setOwingAmount(totalAmount.subtract(accumulated));
                } else {
                    tp.setOwingAmount(splitAmountPerPerson);
                    accumulated = accumulated.add(splitAmountPerPerson);
                }
            } else if ("EXACT".equalsIgnoreCase(record.getSplitType())
                    || "WEIGHT".equalsIgnoreCase(record.getSplitType())
                    || "PERCENT".equalsIgnoreCase(record.getSplitType())) {
                // 如果是 EXACT/WEIGHT/PERCENT 模式，直接使用前端传入的每人金额 (已在前端计算好)
                tp.setOwingAmount(rp.getAmount());
            }

            // 计算该参与者的实付金额 (Paid Amount)
            // 简单模型：假设垫付人支付全款，其他人支付 0。
            // 如果需要支持多人垫付，需要在请求中更复杂的结构支持。
            if (rp.getUserId().equals(record.getPayerId())) {
                tp.setPaidAmount(totalAmount);
            } else {
                tp.setPaidAmount(BigDecimal.ZERO);
            }

            participantMapper.insert(tp);
        }

        return Result.success("记账成功", record);
    }

    /**
     * 获取指定账本的所有交易记录
     *
     * @param ledgerId 账本ID
     * @return 交易列表 (包含分摊参与者详情)
     */
    @Override
    public Result<?> getTransactionsByLedger(Long ledgerId) {
        // 1. 获取交易主表记录，按时间倒序排列
        LambdaQueryWrapper<TransactionRecord> query = new LambdaQueryWrapper<>();
        query.eq(TransactionRecord::getLedgerId, ledgerId)
                .orderByDesc(TransactionRecord::getTransactionDate);
        List<TransactionRecord> records = this.list(query);

        // 2. 填充每笔交易的参与者分摊详情
        for (TransactionRecord r : records) {
            LambdaQueryWrapper<TransactionParticipant> pQuery = new LambdaQueryWrapper<>();
            pQuery.eq(TransactionParticipant::getTransactionId, r.getId());
            r.setParticipants(participantMapper.selectList(pQuery));
        }

        return Result.success(records);
    }
}
