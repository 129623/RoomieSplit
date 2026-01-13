package com.roomiesplit.backend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投票实体类
 */
@Data
@TableName("poll")
public class Poll {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("ledger_id")
    private Long ledgerId;

    private String title;

    private String mode; // VOTE, RANDOM

    private String options; // JSON String

    private String status; // ACTIVE, COMPLETED

    @TableField("created_at")
    private LocalDateTime createdAt;
}
