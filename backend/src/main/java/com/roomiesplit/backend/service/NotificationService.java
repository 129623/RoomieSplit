package com.roomiesplit.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.Notification;

public interface NotificationService extends IService<Notification> {
    Result<?> getMyNotifications(Long userId);

    Result<?> markAsRead(Long notificationId, Long userId);

    void sendNotification(Long userId, String type, String title, String message, String actionUrl);
}
