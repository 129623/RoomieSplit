package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.dto.CreateTransactionRequest;
import com.roomiesplit.backend.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 交易控制器
 */
@RestController
@RequestMapping("/api/v1/ledgers/{ledgerId}/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    /**
     * 创建(记一笔)交易
     *
     * @param ledgerId 账本ID (Path Variable)
     * @param request  交易详情 (包含金额、描述、分摊方式、参与人等)
     * @param userId   操作用户ID (付款人或记录人)
     * @return 交易创建结果
     */
    @PostMapping
    public Result<?> createTransaction(@PathVariable Long ledgerId,
            @RequestBody CreateTransactionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        request.setLedgerId(ledgerId);
        return transactionService.createTransaction(request, userId);
    }

    /**
     * 获取账本内的所有交易列表
     *
     * @param ledgerId 账本ID
     * @return 交易列表 (按时间倒序)
     */
    @GetMapping
    public Result<?> getTransactions(@PathVariable Long ledgerId) {
        return transactionService.getTransactionsByLedger(ledgerId);
    }
}
