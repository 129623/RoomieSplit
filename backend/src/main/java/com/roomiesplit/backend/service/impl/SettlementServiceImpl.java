package com.roomiesplit.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.*;
import com.roomiesplit.backend.mapper.*;
import com.roomiesplit.backend.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SettlementServiceImpl extends ServiceImpl<SettlementMapper, Settlement> implements SettlementService {

    @Autowired
    private TransactionRecordMapper transactionRecordMapper;
    @Autowired
    private TransactionParticipantMapper transactionParticipantMapper;
    @Autowired
    private SettlementMapper settlementMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> smartSettle(Long ledgerId, Long fromUserId, Long toUserId, BigDecimal amount, String method) {
        System.out.println("SmartSettle Request: Ledger " + ledgerId + ", " + fromUserId + " -> " + toUserId
                + ", Amount: " + amount);

        // 1. Build Current Debt Graph
        Map<Long, Map<Long, BigDecimal>> graph = buildDebtGraph(ledgerId);
        System.out.println("Graph Built: " + graph);

        // 2. Find Path from 'fromUserId' to 'toUserId' using BFS
        List<Long> path = findPath(graph, fromUserId, toUserId);
        System.out.println("Path Found: " + path);

        // 3. Create Settlements along the path
        if (path != null && path.size() > 1) {
            for (int i = 0; i < path.size() - 1; i++) {
                Long u = path.get(i);
                Long v = path.get(i + 1);
                System.out.println("Creating Settlement: " + u + " -> " + v);
                createSettlement(ledgerId, u, v, amount, method);
            }
        } else {
            // No path found (or direct), fallback to direct settlement
            System.out.println("No Path / Direct. Creating Direct: " + fromUserId + " -> " + toUserId);
            createSettlement(ledgerId, fromUserId, toUserId, amount, method);
        }

        // 4. Check if debt is fully cleared using helper
        checkAndArchiveIfCleared(ledgerId);

        if (path != null && path.size() > 1) {
            return Result.success("Smart settlement executed along path: " + path);
        } else {
            return Result.success("Direct settlement executed");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> confirmSettlement(Long settlementId) {
        Settlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            return Result.error("Settlement not found");
        }
        if ("COMPLETED".equals(settlement.getStatus())) {
            return Result.error("Settlement already completed");
        }

        settlement.setStatus("COMPLETED");
        settlementMapper.updateById(settlement);

        // Update Notification to remove button and change text
        Notification notifUpdate = new Notification();
        notifUpdate.setActionUrl("");
        notifUpdate.setType("PAYMENT_DONE");
        notifUpdate.setMessage("已确认收款 ¥" + settlement.getAmount());

        LambdaQueryWrapper<Notification> notifQuery = new LambdaQueryWrapper<>();
        notifQuery.eq(Notification::getActionUrl, "app://settlement/confirm?id=" + settlementId);
        notificationMapper.update(notifUpdate, notifQuery);

        // Check auto-archive
        checkAndArchiveIfCleared(settlement.getLedgerId());

        return Result.success("Settlement confirmed");
    }

    @Override
    public Result<?> remindPayment(Long fromUserId, Long toUserId, BigDecimal amount) {
        Notification n = new Notification();
        n.setUserId(toUserId);
        n.setType("PAYMENT_REMINDER");
        n.setTitle("付款提醒");
        n.setMessage("您的室友提醒您支付欠款 ¥" + amount + "，请尽快结算。");
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);

        return Result.success("Reminder sent");
    }

    /**
     * Helper to check net balances and archive if all cleared
     */
    private void checkAndArchiveIfCleared(Long ledgerId) {
        Map<Long, Map<Long, BigDecimal>> newGraph = buildDebtGraph(ledgerId);

        // Calculate Net Balance for each user
        Map<Long, BigDecimal> netBalances = new HashMap<>();
        for (Map.Entry<Long, Map<Long, BigDecimal>> entry : newGraph.entrySet()) {
            Long u = entry.getKey();
            for (Map.Entry<Long, BigDecimal> edge : entry.getValue().entrySet()) {
                Long v = edge.getKey();
                BigDecimal amt = edge.getValue();
                // u owes v: u decreases, v increases
                netBalances.put(u, netBalances.getOrDefault(u, BigDecimal.ZERO).subtract(amt));
                netBalances.put(v, netBalances.getOrDefault(v, BigDecimal.ZERO).add(amt));
            }
        }

        // Check if all net balances are effectively zero
        boolean allCleared = true;
        if (newGraph.isEmpty()) {
            allCleared = true;
        } else {
            for (BigDecimal balance : netBalances.values()) {
                if (balance.abs().compareTo(new BigDecimal("0.01")) >= 0) {
                    allCleared = false;
                    break;
                }
            }
        }

        if (allCleared) {
            System.out.println("All debts cleared (Net Balances ~ 0). Archiving all records...");
            archiveAll(ledgerId);
        }
    }

    private void archiveAll(Long ledgerId) {
        TransactionRecord txUpdate = new TransactionRecord();
        txUpdate.setIsArchived(true);
        LambdaQueryWrapper<TransactionRecord> txQuery = new LambdaQueryWrapper<>();
        txQuery.eq(TransactionRecord::getLedgerId, ledgerId);
        // Only archive active ones
        txQuery.and(w -> w.eq(TransactionRecord::getIsArchived, false).or().isNull(TransactionRecord::getIsArchived));
        transactionRecordMapper.update(txUpdate, txQuery);

        Settlement stUpdate = new Settlement();
        stUpdate.setIsArchived(true);
        LambdaQueryWrapper<Settlement> stQuery = new LambdaQueryWrapper<>();
        stQuery.eq(Settlement::getLedgerId, ledgerId);
        stQuery.and(w -> w.eq(Settlement::getIsArchived, false).or().isNull(Settlement::getIsArchived));
        settlementMapper.update(stUpdate, stQuery);
    }

    @Autowired
    private NotificationMapper notificationMapper;

    private void createSettlement(Long ledgerId, Long from, Long to, BigDecimal amount, String method) {
        Settlement s = new Settlement();
        s.setLedgerId(ledgerId);
        s.setFromUserId(from);
        s.setToUserId(to);
        s.setAmount(amount);
        s.setCurrency("CNY");
        s.setSettledAt(LocalDateTime.now());
        // Status logic
        if ("WECHAT".equalsIgnoreCase(method)) {
            s.setStatus("COMPLETED");
        } else {
            s.setStatus("PENDING");
            // Create Notification for 'to' user (the one receiving money)
            Notification n = new Notification();
            n.setUserId(to);
            n.setType("PAYMENT_CONFIRMATION");
            n.setTitle("线下支付确认");
            n.setMessage("室友申请了 ¥" + amount + " 的线下还款，请确认是否收到。");
            n.setIsRead(false);
            // We can put the settlement ID in actionUrl for future implementation
            // URL scheme: app://settlement/confirm?id={id}
            // But since ID is not generated until insert, we might need to insert first.
            // Actually, Mybatis Plus populates ID after insert.
        }
        settlementMapper.insert(s);

        if ("PENDING".equals(s.getStatus())) {
            Notification n = new Notification();
            n.setUserId(to);
            n.setType("PAYMENT_CONFIRMATION");
            n.setTitle("线下支付确认");
            n.setMessage("室友申请了 ¥" + amount + " 的线下还款，请确认是否收到。");
            n.setActionUrl("app://settlement/confirm?id=" + s.getId());
            n.setIsRead(false);
            n.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(n);
        }
    }

    // BFS to find path
    private List<Long> findPath(Map<Long, Map<Long, BigDecimal>> graph, Long start, Long end) {
        if (!graph.containsKey(start))
            return null;

        Queue<List<Long>> queue = new LinkedList<>();
        queue.add(Collections.singletonList(start));
        Set<Long> visited = new HashSet<>();
        visited.add(start);

        while (!queue.isEmpty()) {
            List<Long> path = queue.poll();
            Long node = path.get(path.size() - 1);

            if (node.equals(end)) {
                return path;
            }

            Map<Long, BigDecimal> neighbors = graph.get(node);
            if (neighbors != null) {
                for (Long neighbor : neighbors.keySet()) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        List<Long> newPath = new ArrayList<>(path);
                        newPath.add(neighbor);
                        queue.add(newPath);
                    }
                }
            }
        }
        return null; // No path found
    }

    // Helper to build graph (Simplified logic from ReportService)
    private Map<Long, Map<Long, BigDecimal>> buildDebtGraph(Long ledgerId) {
        // Collect debts
        LambdaQueryWrapper<TransactionRecord> txQuery = new LambdaQueryWrapper<>();
        txQuery.eq(TransactionRecord::getLedgerId, ledgerId);
        // exclude archived
        txQuery.and(w -> w.eq(TransactionRecord::getIsArchived, false).or().isNull(TransactionRecord::getIsArchived));
        List<TransactionRecord> transactions = transactionRecordMapper.selectList(txQuery);
        Map<Long, Long> txPayerMap = transactions.stream()
                .collect(Collectors.toMap(TransactionRecord::getId, TransactionRecord::getPayerId));
        List<Long> txIds = transactions.stream().map(TransactionRecord::getId).collect(Collectors.toList());

        Map<String, BigDecimal> debtMap = new HashMap<>();

        if (!txIds.isEmpty()) {
            LambdaQueryWrapper<TransactionParticipant> partQuery = new LambdaQueryWrapper<>();
            partQuery.in(TransactionParticipant::getTransactionId, txIds);
            List<TransactionParticipant> parts = transactionParticipantMapper.selectList(partQuery);
            for (TransactionParticipant p : parts) {
                Long payerId = txPayerMap.get(p.getTransactionId());
                if (payerId != null && !payerId.equals(p.getUserId())) {
                    String k = p.getUserId() + "_" + payerId;
                    debtMap.put(k, debtMap.getOrDefault(k, BigDecimal.ZERO).add(p.getOwingAmount()));
                }
            }
        }

        // Subtract Settlements
        LambdaQueryWrapper<Settlement> setQuery = new LambdaQueryWrapper<>();
        setQuery.eq(Settlement::getLedgerId, ledgerId);
        // exclude archived
        setQuery.and(w -> w.eq(Settlement::getIsArchived, false).or().isNull(Settlement::getIsArchived));
        List<Settlement> settlements = settlementMapper.selectList(setQuery);
        for (Settlement s : settlements) {
            if (!"COMPLETED".equalsIgnoreCase(s.getStatus())) {
                continue;
            }
            String k = s.getFromUserId() + "_" + s.getToUserId();
            debtMap.put(k, debtMap.getOrDefault(k, BigDecimal.ZERO).subtract(s.getAmount()));
        }

        // Build Graph Adjacency List (Only positive debts)
        Map<Long, Map<Long, BigDecimal>> adj = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : debtMap.entrySet()) {
            String[] tokens = entry.getKey().split("_");
            Long u = Long.parseLong(tokens[0]);
            Long v = Long.parseLong(tokens[1]); // u -> v
            BigDecimal amt = entry.getValue();

            if (amt.compareTo(BigDecimal.ZERO) == 0)
                continue;

            if (amt.compareTo(BigDecimal.ZERO) > 0) {
                // u owes v
                adj.computeIfAbsent(u, k -> new HashMap<>()).merge(v, amt, BigDecimal::add);
            } else {
                // amt < 0 => v owes u
                adj.computeIfAbsent(v, k -> new HashMap<>()).merge(u, amt.abs(), BigDecimal::add);
            }
        }
        return adj;
    }
}
