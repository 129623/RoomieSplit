package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.dto.CreateLedgerRequest;
import com.roomiesplit.backend.service.LedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 账本控制器
 */
@RestController
@RequestMapping("/api/v1/ledgers")
@CrossOrigin(origins = "*")
public class LedgerController {

    @Autowired
    private LedgerService ledgerService;

    /**
     * 获取我的账本列表
     * <p>
     * 查询当前用户参与的所有账本 (包括创建的和被邀请加入的)
     *
     * @param userId 当前用户ID (从Header获取)
     * @return 账本列表
     */
    @GetMapping
    public Result<?> getMyLedgers(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            // fallback: 如果 Header 为空尝试从其他途径获取，此处直接返回未登录
            return Result.error(401, "请先登录 (Missing X-User-Id header)");
        }
        return ledgerService.getMyLedgers(userId);
    }

    /**
     * 创建新账本
     *
     * @param request 包含账本名称和描述
     * @param userId  创建者ID
     * @return 创建成功的账本信息
     */
    @PostMapping
    public Result<?> createLedger(@RequestBody CreateLedgerRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return ledgerService.createLedger(request, userId);
    }

    /**
     * 获取账本详情 (含成员列表)
     *
     * @param id     账本ID
     * @param userId 当前用户ID (用于权限检查)
     * @return 账本详情 + 成员列表
     */
    @GetMapping("/{id}")
    public Result<?> getLedgerDetail(@PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        return ledgerService.getLedgerDetail(id, userId);
    }

    @PostMapping("/{id}/members/status")
    public Result<?> updateStatus(@PathVariable Long id,
            @RequestBody com.roomiesplit.backend.dto.UpdateMemberStatusRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        if (userId == null)
            return Result.error(401, "Login required");
        return ledgerService.updateMemberStatus(id, userId, request.getStatus());
    }

    // inviteMember 方法已移动到 InvitationController 以避免路径冲突
    // 该控制器专注于账本自身的 CRUD 操作
}
