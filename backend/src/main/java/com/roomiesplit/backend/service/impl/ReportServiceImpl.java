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

        // 2.1 获取该账本所有交易记录 (排除已归档)
        LambdaQueryWrapper<TransactionRecord> txQuery = new LambdaQueryWrapper<>();
        txQuery.eq(TransactionRecord::getLedgerId, ledgerId);
        txQuery.and(wrapper -> wrapper.eq(TransactionRecord::getIsArchived, false).or()
                .isNull(TransactionRecord::getIsArchived));
        List<TransactionRecord> transactions = transactionRecordMapper.selectList(txQuery);
        // 建立 交易ID -> 付款人ID 的映射
        // 建立 交易ID -> 付款人ID 的映射
        Map<Long, Long> txPayerMap = transactions.stream()
                .filter(t -> t.getPayerId() != null)
                .collect(Collectors.toMap(TransactionRecord::getId, TransactionRecord::getPayerId));

        List<Long> txIds = transactions.stream().map(TransactionRecord::getId).collect(Collectors.toList());
        List<Map<String, Object>> edges = new ArrayList<>();
        List<Map<String, Object>> simplifiedEdges = new ArrayList<>();

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
            // 排除已归档的结算
            LambdaQueryWrapper<Settlement> setQuery = new LambdaQueryWrapper<>();
            setQuery.eq(Settlement::getLedgerId, ledgerId);
            setQuery.and(
                    wrapper -> wrapper.eq(Settlement::getIsArchived, false).or().isNull(Settlement::getIsArchived));
            List<Settlement> settlements = settlementMapper.selectList(setQuery);

            for (Settlement s : settlements) {
                // Only count COMPLETED settlements
                if (!"COMPLETED".equalsIgnoreCase(s.getStatus())) {
                    continue;
                }
                // 结算 A->B，意味着 A 给 B 钱，抵消 A 对 B 的同等债务
                String key = s.getFromUserId() + "_" + s.getToUserId();
                BigDecimal current = debtMap.getOrDefault(key, BigDecimal.ZERO);
                debtMap.put(key, current.subtract(s.getAmount()));
            }

            // 2.4 整理债务方向 (Process Directions)
            // 不进行双向抵消，但处理负值 (负值表示反向债务)
            // 例如 A->B -20 意味着 B->A 20
            // 合并相同方向的债务
            Map<String, BigDecimal> formattedDebt = new HashMap<>();

            for (Map.Entry<String, BigDecimal> entry : debtMap.entrySet()) {
                String[] parts = entry.getKey().split("_");
                Long fromId = Long.parseLong(parts[0]);
                Long toId = Long.parseLong(parts[1]);
                BigDecimal amount = entry.getValue();

                if (amount.compareTo(BigDecimal.ZERO) == 0)
                    continue;

                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    // from -> to
                    String k = fromId + "_" + toId;
                    formattedDebt.put(k, formattedDebt.getOrDefault(k, BigDecimal.ZERO).add(amount));
                } else {
                    // amount < 0, implies to -> from
                    String k = toId + "_" + fromId;
                    formattedDebt.put(k, formattedDebt.getOrDefault(k, BigDecimal.ZERO).add(amount.abs()));
                }
            }

            // 2.5 构建最终的边数据 (原始 - 保留双向)
            for (Map.Entry<String, BigDecimal> entry : formattedDebt.entrySet()) {
                String[] parts = entry.getKey().split("_");
                Long u1 = Long.parseLong(parts[0]);
                Long u2 = Long.parseLong(parts[1]);
                BigDecimal val = entry.getValue();

                if (val.compareTo(BigDecimal.ZERO) == 0)
                    continue;

                Map<String, Object> edge = new HashMap<>();
                edge.put("from", u1);
                edge.put("to", u2);
                edge.put("amount", val);
                edges.add(edge);
            }

            // 3. 计算简化后的债务图 (Simplified/Optimized)
            // 简化算法内部会计算净余额，所以传入包含双向债务的 Map 也是可以的，算法会自动处理
            simplifiedEdges = com.roomiesplit.backend.utils.DebtSimplificationStrategy.simplify(formattedDebt);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("originalEdges", edges);
        result.put("simplifiedEdges", simplifiedEdges);
        result.put("edges", edges); // Backward compatibility default to original

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
