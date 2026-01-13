package com.roomiesplit.backend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 交易参与者实体类
 */
@Data
@TableName("transaction_participant")
public class TransactionParticipant {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("transaction_id")
    private Long transactionId;

    @TableField("user_id")
    private Long userId;

    @TableField("owing_amount")
    private BigDecimal owingAmount;

    @TableField("paid_amount")
    private BigDecimal paidAmount;
}
