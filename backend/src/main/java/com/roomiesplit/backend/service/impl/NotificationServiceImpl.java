package com.roomiesplit.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.Notification;
import com.roomiesplit.backend.mapper.NotificationMapper;
import com.roomiesplit.backend.service.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    /**
     * 获取指定用户的所有通知
     *
     * @param userId 用户ID
     * @return 通知列表
     */
    @Override
    public Result<?> getMyNotifications(Long userId) {
        // 查询用户的通知，按时间倒序排列
        LambdaQueryWrapper<Notification> query = new LambdaQueryWrapper<>();
        query.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);
        return Result.success(this.list(query));
    }

    /**
     * 标记通知为已读
     *
     * @param notificationId 通知ID
     * @param userId         操作用户ID
     * @return 结果
     */
    @Override
    public Result<?> markAsRead(Long notificationId, Long userId) {
        Notification notification = this.getById(notificationId);
        if (notification == null) {
            return Result.error(404, "通知不存在");
        }
        // 权限检查
        if (!notification.getUserId().equals(userId)) {
            return Result.error(403, "无权操作");
        }
        notification.setIsRead(true);
        this.updateById(notification);
        return Result.success("已标记为已读");
    }

    /**
     * 发送系统内部通知
     *
     * @param userId    接收用户ID
     * @param type      通知类型 (如 INVITE, SYSTEM, ALERT)
     * @param title     标题
     * @param message   内容
     * @param actionUrl 动作链接或携带的数据 (如 invitationToken)
     */
    @Override
    public void sendNotification(Long userId, String type, String title, String message, String actionUrl) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setActionUrl(actionUrl);
        notification.setCreatedAt(LocalDateTime.now());
        this.save(notification);
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.roomiesplit.backend.mapper.LedgerMemberMapper ledgerMemberMapper;

    @Override
    public void broadcastNotification(Long ledgerId, String type, String title, String message, String actionUrl) {
        LambdaQueryWrapper<com.roomiesplit.backend.domain.LedgerMember> query = new LambdaQueryWrapper<>();
        query.eq(com.roomiesplit.backend.domain.LedgerMember::getLedgerId, ledgerId);
        java.util.List<com.roomiesplit.backend.domain.LedgerMember> members = ledgerMemberMapper.selectList(query);

        for (com.roomiesplit.backend.domain.LedgerMember member : members) {
            sendNotification(member.getUserId(), type, title, message, actionUrl);
        }
    }
}
