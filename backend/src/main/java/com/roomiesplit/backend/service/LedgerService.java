package com.roomiesplit.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.Ledger;
import com.roomiesplit.backend.dto.CreateLedgerRequest;

/**
 * 账本服务接口
 */
public interface LedgerService extends IService<Ledger> {

    /**
     * 创建账本
     * 
     * @param request 创建请求
     * @param userId  当前用户ID
     * @return 结果
     */
    Result<?> createLedger(CreateLedgerRequest request, Long userId);

    /**
     * 获取用户的所有账本
     * 
     * @param userId 用户ID
     * @return 账本列表
     */
    Result<?> getMyLedgers(Long userId);

    /**
     * 获取账本详情
     * 
     * @param ledgerId 账本ID
     * @param userId   当前用户ID (用于权限检查)
     * @return 详情
     */
    Result<?> getLedgerDetail(Long ledgerId, Long userId);

    /**
     * 邀请成员
     */
    Result<?> inviteMember(Long ledgerId, String email, Long inviterId);

    Result<?> updateMemberStatus(Long ledgerId, Long userId, String status);
}
