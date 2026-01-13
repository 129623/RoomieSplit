package com.roomiesplit.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.TransactionRecord;
import com.roomiesplit.backend.dto.CreateTransactionRequest;

/**
 * 交易服务接口
 */
public interface TransactionService extends IService<TransactionRecord> {

    /**
     * 创建交易 (记账)
     * 
     * @param request 请求
     * @param userId  操作人ID (通常是付款人或记录人)
     * @return 结果
     */
    Result<?> createTransaction(CreateTransactionRequest request, Long userId);

    /**
     * 获取账本的交易列表
     * 
     * @param ledgerId 账本ID
     * @return 列表
     */
    Result<?> getTransactionsByLedger(Long ledgerId);
}
