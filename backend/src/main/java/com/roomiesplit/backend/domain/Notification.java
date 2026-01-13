package com.roomiesplit.backend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知实体类
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String type; // PAYMENT_REMINDER, INVITE_REQUEST

    private String title;

    private String message;

    @TableField("is_read")
    private Boolean isRead;

    @TableField("action_url")
    private String actionUrl;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
