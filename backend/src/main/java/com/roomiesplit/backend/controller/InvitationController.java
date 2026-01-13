package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.dto.InviteRequest;
import com.roomiesplit.backend.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class InvitationController {

    @Autowired
    private InvitationService invitationService;

    /**
     * 发送账本邀请
     *
     * @param ledgerId 账本ID
     * @param request  邀请请求体 (含被邀请人邮箱)
     * @param userId   操作用户ID
     * @return 操作结果
     */
    @PostMapping("/ledgers/{ledgerId}/invite")
    public Result<?> invite(@PathVariable Long ledgerId,
            @RequestBody InviteRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return invitationService.invite(ledgerId, request, userId);
    }

    /**
     * 接受邀请
     *
     * @param token  邀请Token (从链接或消息中获取)
     * @param userId 操作用户ID
     * @return 操作结果
     */
    // Using token in URL for acceptance
    @PostMapping("/invitations/{token}/accept")
    public Result<?> accept(@PathVariable String token,
            @RequestHeader("X-User-Id") Long userId) {
        return invitationService.accept(token, userId);
    }

    /**
     * 拒绝邀请
     *
     * @param token  邀请Token
     * @param userId 操作用户ID
     * @return 操作结果
     */
    @PostMapping("/invitations/{token}/reject")
    public Result<?> reject(@PathVariable String token,
            @RequestHeader("X-User-Id") Long userId) {
        return invitationService.reject(token, userId);
    }
}
