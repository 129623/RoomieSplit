package com.roomiesplit.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.Invitation;
import com.roomiesplit.backend.domain.LedgerMember;
import com.roomiesplit.backend.dto.InviteRequest;
import com.roomiesplit.backend.mapper.InvitationMapper;
import com.roomiesplit.backend.mapper.LedgerMemberMapper;
import com.roomiesplit.backend.service.InvitationService;
import com.roomiesplit.backend.service.NotificationService;
import com.roomiesplit.backend.domain.SysUser;
import com.roomiesplit.backend.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InvitationServiceImpl extends ServiceImpl<InvitationMapper, Invitation> implements InvitationService {

    @Autowired
    private LedgerMemberMapper ledgerMemberMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 发送账本邀请
     *
     * @param ledgerId 账本ID
     * @param request  邀请请求，包含被邀请人邮箱
     * @param senderId 发送邀请的用户ID
     * @return 结果
     */
    @Override
    public Result<?> invite(Long ledgerId, InviteRequest request, Long senderId) {
        // 1. 检查是否已有待处理的邀请 (防止重复邀请)
        LambdaQueryWrapper<Invitation> query = new LambdaQueryWrapper<>();
        query.eq(Invitation::getLedgerId, ledgerId)
                .eq(Invitation::getEmail, request.getEmail())
                .eq(Invitation::getStatus, "PENDING");
        if (this.count(query) > 0) {
            return Result.error(409, "该用户已被邀请");
        }

        // 2. 创建邀请记录并生成唯一 Token
        Invitation invitation = new Invitation();
        invitation.setLedgerId(ledgerId);
        invitation.setSenderId(senderId);
        invitation.setEmail(request.getEmail());
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus("PENDING");
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7天过期

        this.save(invitation);

        // 3. 发送应用内通知 (携带 Token)
        LambdaQueryWrapper<SysUser> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(SysUser::getEmail, request.getEmail());
        SysUser invitee = sysUserMapper.selectOne(userQuery);

        if (invitee != null) {
            String title = "室友邀请";
            String message = "您收到一个新的账本邀请, 点击同意加入.";
            // 这里的 "INVITE" 类型和 Token 将被前端用于显示 "接受/拒绝" 按钮
            notificationService.sendNotification(invitee.getId(), "INVITE", title, message, invitation.getToken());
        }

        return Result.success("邀请已发送", invitation);
    }

    /**
     * 接受邀请
     *
     * @param token  邀请Token
     * @param userId 当前操作用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public Result<?> accept(String token, Long userId) {
        // 1. 验证邀请有效性（存在、状态为PENDING、未过期）
        LambdaQueryWrapper<Invitation> query = new LambdaQueryWrapper<>();
        query.eq(Invitation::getToken, token);
        Invitation invitation = this.getOne(query);

        if (invitation == null) {
            return Result.error(404, "邀请不存在");
        }
        if (!"PENDING".equals(invitation.getStatus())) {
            return Result.error(400, "邀请已失效");
        }
        if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            invitation.setStatus("EXPIRED");
            this.updateById(invitation);
            return Result.error(400, "邀请已过期");
        }

        // 2. 将用户添加到账本成员列表
        // 先检查是否已经是成员，防止重复添加
        LambdaQueryWrapper<LedgerMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(LedgerMember::getLedgerId, invitation.getLedgerId())
                .eq(LedgerMember::getUserId, userId);
        if (ledgerMemberMapper.selectCount(memberQuery) == 0) {
            LedgerMember member = new LedgerMember();
            member.setLedgerId(invitation.getLedgerId());
            member.setUserId(userId);
            member.setRole("MEMBER");
            member.setStatus("ACTIVE");
            member.setJoinedAt(LocalDateTime.now());
            ledgerMemberMapper.insert(member);
        }

        // 3. 更新邀请状态为 ACCEPTED
        invitation.setStatus("ACCEPTED");
        this.updateById(invitation);

        // 4. 通知邀请发起人
        notificationService.sendNotification(invitation.getSenderId(),
                "INVITE_ACCEPTED",
                "邀请被接受",
                "用户 ID:" + userId + " 接受了您的邀请",
                "");

        // 5. 更新原邀请通知的状态，防止按钮重复显示
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.roomiesplit.backend.domain.Notification> updateNotif = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateNotif.eq(com.roomiesplit.backend.domain.Notification::getActionUrl, token)
                .set(com.roomiesplit.backend.domain.Notification::getType, "INVITE_ACCEPTED") // 修改类型
                .set(com.roomiesplit.backend.domain.Notification::getActionUrl, "") // 清除Token/URL
                .set(com.roomiesplit.backend.domain.Notification::getMessage, "您已接受邀请，成功加入账本");
        notificationService.update(updateNotif);

        return Result.success("加入成功");
    }

    /**
     * 拒绝邀请
     *
     * @param token  邀请Token
     * @param userId 当前操作用户ID
     * @return 结果
     */
    @Override
    public Result<?> reject(String token, Long userId) {
        LambdaQueryWrapper<Invitation> query = new LambdaQueryWrapper<>();
        query.eq(Invitation::getToken, token);
        Invitation invitation = this.getOne(query);

        if (invitation == null) {
            return Result.error(404, "邀请不存在");
        }

        // 更新状态为 REJECTED
        invitation.setStatus("REJECTED");
        this.updateById(invitation);

        // 更新原邀请通知的状态
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.roomiesplit.backend.domain.Notification> updateNotif = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateNotif.eq(com.roomiesplit.backend.domain.Notification::getActionUrl, token)
                .set(com.roomiesplit.backend.domain.Notification::getType, "INVITE_REJECTED")
                .set(com.roomiesplit.backend.domain.Notification::getActionUrl, "")
                .set(com.roomiesplit.backend.domain.Notification::getMessage, "您已拒绝邀请");
        notificationService.update(updateNotif);

        return Result.success("已拒绝邀请");
    }
}
