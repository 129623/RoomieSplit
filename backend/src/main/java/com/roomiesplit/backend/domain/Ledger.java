package com.roomiesplit.backend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账本/群组实体类
 */
@Data
@TableName("ledger")
public class Ledger {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账本名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 拥有者ID
     */
    @TableField("owner_id")
    private Long ownerId;

    /**
     * 默认货币
     */
    @TableField("default_currency")
    private String defaultCurrency;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
