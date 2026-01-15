package com.roomiesplit.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.Settlement;
import java.math.BigDecimal;

public interface SettlementService extends IService<Settlement> {
    /**
     * Smart Settle: Find path in graph and create settlements
     */
    Result<?> smartSettle(Long ledgerId, Long fromUserId, Long toUserId, BigDecimal amount, String method);

    /**
     * Confirm offline settlement
     */
    Result<?> confirmSettlement(Long settlementId);

    /**
     * Remind payment
     */
    Result<?> remindPayment(Long fromUserId, Long toUserId, BigDecimal amount);
}
