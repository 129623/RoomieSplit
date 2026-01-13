package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取我的通知列表
     *
     * @param userId 当前用户ID
     * @return 通知列表
     */
    @GetMapping
    public Result<?> getMyNotifications(@RequestHeader("X-User-Id") Long userId) {
        return notificationService.getMyNotifications(userId);
    }

    /**
     * 标记通知为已读
     *
     * @param id     通知ID
     * @param userId 当前用户ID
     * @return 操作结果
     */
    @PostMapping("/{id}/read")
    public Result<?> markAsRead(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return notificationService.markAsRead(id, userId);
    }
}
