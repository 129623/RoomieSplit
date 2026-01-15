package com.roomiesplit.backend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易/账单实体类
 */
@Data
@TableName("transaction_record")
public class TransactionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("ledger_id")
    private Long ledgerId;

    @TableField("payer_id")
    private Long payerId;

    private BigDecimal amount;

    private String currency;

    private String category;

    private String description;

    @TableField("transaction_date")
    private LocalDateTime transactionDate;

    /**
     * 分摊方式: EQUAL, EXACT, WEIGHTED
     */
    @TableField("split_type")
    private String splitType;

    @TableField("image_urls")
    private String imageUrls;

    @TableField("is_archived")
    private Boolean isArchived;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private java.util.List<TransactionParticipant> participants;
}
