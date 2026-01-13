package com.roomiesplit.backend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请实体类
 */
@Data
@TableName("invitation")
public class Invitation {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("ledger_id")
    private Long ledgerId;

    @TableField("sender_id")
    private Long senderId;

    private String email;

    private String token;

    private String status; // PENDING, ACCEPTED, REJECTED, EXPIRED

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("expires_at")
    private LocalDateTime expiresAt;
}
