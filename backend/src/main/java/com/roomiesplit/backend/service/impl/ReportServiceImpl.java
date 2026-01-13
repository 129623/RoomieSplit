package com.roomiesplit.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.LedgerMember;
import com.roomiesplit.backend.domain.Settlement;
import com.roomiesplit.backend.domain.SysUser;
import com.roomiesplit.backend.domain.TransactionParticipant;
import com.roomiesplit.backend.domain.TransactionRecord;
import com.roomiesplit.backend.mapper.LedgerMemberMapper;
import com.roomiesplit.backend.mapper.SettlementMapper;
import com.roomiesplit.backend.mapper.SysUserMapper;
import com.roomiesplit.backend.mapper.TransactionParticipantMapper;
import com.roomiesplit.backend.mapper.TransactionRecordMapper;
import com.roomiesplit.backend.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private TransactionRecordMapper transactionRecordMapper;

    @Autowired
    private TransactionParticipantMapper transactionParticipantMapper;

    @Autowired
    private SettlementMapper settlementMapper;

    @Autowired
    private LedgerMemberMapper ledgerMemberMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 获取账本的债务关系图谱
     *
     * @param ledgerId 账本ID
     * @return 包含节点(Nodes)和边(Edges)的图数据，Nodes对应成员，Edges对应债务关系
     */
    @Override
    public Result<?> getDebtGraph(Long ledgerId) {
        // 1. 获取账本所有成员作为图的节点 (Nodes)
        LambdaQueryWrapper<LedgerMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(LedgerMember::getLedgerId, ledgerId);
        List<LedgerMember> members = ledgerMemberMapper.selectList(memberQuery);
        List<Long> userIds = members.stream().map(LedgerMember::getUserId).collect(Collectors.toList());

        List<SysUser> users = userIds.isEmpty() ? Collections.emptyList() : sysUserMapper.selectBatchIds(userIds);
        List<Map<String, Object>> nodes = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getDisplayName());
            map.put("avatar", u.getAvatarUrl());
            return map;
        }).collect(Collectors.toList());

        // 2. 计算债务关系边 (Edges)
        // 逻辑：Transaction 产生债务，Settlement 消除债务

        // 2.1 获取该账本所有交易记录
        LambdaQueryWrapper<TransactionRecord> txQuery = new LambdaQueryWrapper<>();
        txQuery.eq(TransactionRecord::getLedgerId, ledgerId);
        List<TransactionRecord> transactions = transactionRecordMapper.selectList(txQuery);
        // 建立 交易ID -> 付款人ID 的映射
        Map<Long, Long> txPayerMap = transactions.stream()
                .collect(Collectors.toMap(TransactionRecord::getId, TransactionRecord::getPayerId));

        List<Long> txIds = transactions.stream().map(TransactionRecord::getId).collect(Collectors.toList());
        List<Map<String, Object>> edges = new ArrayList<>();

        if (!txIds.isEmpty()) {
            LambdaQueryWrapper<TransactionParticipant> partQuery = new LambdaQueryWrapper<>();
            partQuery.in(TransactionParticipant::getTransactionId, txIds);
            List<TransactionParticipant> participants = transactionParticipantMapper.selectList(partQuery);

            // 2.2 聚合原始债务：Map<"FromUserId_ToUserId", Amount>
            // Key 格式: "欠款人ID_债权人ID"
            Map<String, BigDecimal> debtMap = new HashMap<>();

            for (TransactionParticipant p : participants) {
                Long payerId = txPayerMap.get(p.getTransactionId());
                if (payerId == null)
                    continue;
                if (payerId.equals(p.getUserId()))
                    continue; // 自己欠自己的忽略（如果是自己垫付且自己参与，相当于自己付了自己那份）

                // 债务关系：参与者(Owing) -> 垫付者(Paid)
                String key = p.getUserId() + "_" + payerId;
                BigDecimal current = debtMap.getOrDefault(key, BigDecimal.ZERO);
                debtMap.put(key, current.add(p.getOwingAmount()));
            }

            // 2.3 处理结算记录 (Settlements)
            // 结算通常指 A 向 B 支付金额。这将减少 A 对 B 的债务。
            LambdaQueryWrapper<Settlement> setQuery = new LambdaQueryWrapper<>();
            setQuery.eq(Settlement::getLedgerId, ledgerId);
            List<Settlement> settlements = settlementMapper.selectList(setQuery);

            for (Settlement s : settlements) {
                // 结算 A->B，意味着 A 给 B 钱，抵消 A 对 B 的同等债务
                String key = s.getFromUserId() + "_" + s.getToUserId();
                BigDecimal current = debtMap.getOrDefault(key, BigDecimal.ZERO);
                debtMap.put(key, current.subtract(s.getAmount()));
            }

            // 2.4 债务对冲与归一化 (Consolidate Debts)
            // 如果 A->B 为 100，B->A 为 40，则净债务为 A->B 60
            // 使用 "MinId_MaxId" 作为唯一键来合并双向债务
            Map<String, BigDecimal> netDebt = new HashMap<>();

            for (Map.Entry<String, BigDecimal> entry : debtMap.entrySet()) {
                String[] parts = entry.getKey().split("_");
                Long fromId = Long.parseLong(parts[0]);
                Long toId = Long.parseLong(parts[1]);
                BigDecimal amount = entry.getValue(); // From->To (正数表示欠款，负数因结算可能出现超额还款，需根据逻辑处理)

                if (amount.compareTo(BigDecimal.ZERO) == 0)
                    continue;

                if (fromId < toId) {
                    String k = fromId + "_" + toId;
                    netDebt.put(k, netDebt.getOrDefault(k, BigDecimal.ZERO).add(amount));
                } else {
                    String k = toId + "_" + fromId; // 反转方向累加时需取负
                    netDebt.put(k, netDebt.getOrDefault(k, BigDecimal.ZERO).subtract(amount));
                }
            }

            // 2.5 构建最终的边数据
            for (Map.Entry<String, BigDecimal> entry : netDebt.entrySet()) {
                String[] parts = entry.getKey().split("_");
                Long u1 = Long.parseLong(parts[0]);
                Long u2 = Long.parseLong(parts[1]);
                BigDecimal val = entry.getValue();

                if (val.compareTo(BigDecimal.ZERO) > 0) {
                    // u1 -> u2 (u1 欠 u2)
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("from", u1);
                    edge.put("to", u2);
                    edge.put("amount", val);
                    edges.add(edge);
                } else if (val.compareTo(BigDecimal.ZERO) < 0) {
                    // u2 -> u1 (u2 欠 u1，因为值是负的)
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("from", u2);
                    edge.put("to", u1);
                    edge.put("amount", val.abs());
                    edges.add(edge);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);

        return Result.success(result);
    }

    /**
     * 获取指定时间范围内的报表交易数据
     *
     * @param ledgerId  账本ID
     * @param startDate 开始日期 (可选)
     * @param endDate   结束日期 (可选)
     * @return 交易列表
     */
    @Override
    public Result<?> getReportData(Long ledgerId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<TransactionRecord> query = new LambdaQueryWrapper<>();
        query.eq(TransactionRecord::getLedgerId, ledgerId);

        if (startDate != null) {
            query.ge(TransactionRecord::getTransactionDate, startDate.atStartOfDay());
        }
        if (endDate != null) {
            query.le(TransactionRecord::getTransactionDate, endDate.atTime(23, 59, 59));
        }
        query.orderByDesc(TransactionRecord::getTransactionDate);

        List<TransactionRecord> list = transactionRecordMapper.selectList(query);
        return Result.success(list);
    }
}
