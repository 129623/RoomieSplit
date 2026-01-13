package com.roomiesplit.backend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Karma 记录实体类
 */
@Data
@TableName("karma_record")
public class KarmaRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("ledger_id")
    private Long ledgerId;

    @TableField("user_id")
    private Long userId;

    private Integer points;

    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
