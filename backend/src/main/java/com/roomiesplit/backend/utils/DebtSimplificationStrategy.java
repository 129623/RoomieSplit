package com.roomiesplit.backend.utils;

import java.math.BigDecimal;
import java.util.*;

public class DebtSimplificationStrategy {

    /**
     * Calculates the minimized transactions to settle all debts.
     * Uses a greedy algorithm: matches the largest debtor with the largest
     * creditor.
     *
     * @param netDebtMap Map of "fromUserId_toUserId" -> Amount
     * @return List of edges (maps) representing the simplified transactions
     */
    public static List<Map<String, Object>> simplify(Map<String, BigDecimal> netDebtMap) {
        // 1. Calculate net balance for each user
        Map<Long, BigDecimal> balanceMap = new HashMap<>();

        for (Map.Entry<String, BigDecimal> entry : netDebtMap.entrySet()) {
            String[] parts = entry.getKey().split("_");
            Long fromId = Long.parseLong(parts[0]);
            Long toId = Long.parseLong(parts[1]);
            BigDecimal amount = entry.getValue();

            // From owes To: From's balance decreases, To's balance increases
            balanceMap.put(fromId, balanceMap.getOrDefault(fromId, BigDecimal.ZERO).subtract(amount));
            balanceMap.put(toId, balanceMap.getOrDefault(toId, BigDecimal.ZERO).add(amount));
        }

        // 2. Separate into Debtors and Creditors
        List<Long> debtors = new ArrayList<>();
        List<Long> creditors = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : balanceMap.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(entry.getKey());
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(entry.getKey());
            }
        }

        // 3. Greedy Matching
        List<Map<String, Object>> resultEdges = new ArrayList<>();

        // Sort by absolute amount helps in greedy efficiency (optional but recommended)
        // Note: For exact minimizing of *number* of transactions, it's NP-hard.
        // Greedy approach is a standard approximation.
        // We handle lists dynamically below, sorting might not be strictly preserved
        // after updates unless re-sorted.
        // For simplicity, we just iterate.

        int i = 0; // debtor index
        int j = 0; // creditor index

        while (i < debtors.size() && j < creditors.size()) {
            Long debtorId = debtors.get(i);
            Long creditorId = creditors.get(j);

            BigDecimal debtorBalance = balanceMap.get(debtorId); // Negative
            BigDecimal creditorBalance = balanceMap.get(creditorId); // Positive

            BigDecimal amountToSettle = debtorBalance.abs().min(creditorBalance);

            // Record transaction: Debtor -> Creditor
            Map<String, Object> edge = new HashMap<>();
            edge.put("from", debtorId);
            edge.put("to", creditorId);
            edge.put("amount", amountToSettle);
            resultEdges.add(edge);

            // Update balances
            BigDecimal newDebtorBalance = debtorBalance.add(amountToSettle);
            BigDecimal newCreditorBalance = creditorBalance.subtract(amountToSettle);

            balanceMap.put(debtorId, newDebtorBalance);
            balanceMap.put(creditorId, newCreditorBalance);

            // Move pointers if settled (using a small epsilon for float comparison safety,
            // though BigDecimal is exact)
            if (newDebtorBalance.abs().compareTo(new BigDecimal("0.001")) < 0) {
                i++;
            }
            if (newCreditorBalance.abs().compareTo(new BigDecimal("0.001")) < 0) {
                j++;
            }
        }

        return resultEdges;
    }
}
