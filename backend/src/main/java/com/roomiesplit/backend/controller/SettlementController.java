package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settlements")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    @PostMapping("/smart")
    public Result<?> createSmartSettlement(@RequestBody Map<String, Object> payload) {
        Long ledgerId = Long.valueOf(payload.get("ledgerId").toString());
        Long fromUserId = Long.valueOf(payload.get("fromUserId").toString());
        Long toUserId = Long.valueOf(payload.get("toUserId").toString());
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        String method = payload.get("method").toString(); // WECHAT, OFFLINE

        return settlementService.smartSettle(ledgerId, fromUserId, toUserId, amount, method);
    }

    @PostMapping("/confirm")
    public Result<?> confirmSettlement(@RequestParam Long id) {
        return settlementService.confirmSettlement(id);
    }

    @PostMapping("/remind")
    public Result<?> remindPayment(@RequestBody Map<String, Object> payload) {
        Long fromUserId = Long.valueOf(payload.get("fromUserId").toString());
        Long toUserId = Long.valueOf(payload.get("toUserId").toString());
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());
        return settlementService.remindPayment(fromUserId, toUserId, amount);
    }
}
