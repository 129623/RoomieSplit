package com.roomiesplit.backend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账本成员实体类
 */
@Data
@TableName("ledger_member")
public class LedgerMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("ledger_id")
    private Long ledgerId;

    @TableField("user_id")
    private Long userId;

    /**
     * 角色: OWNER, ADMIN, MEMBER
     */
    private String role;

    /**
     * 状态: ACTIVE, INVITED
     */
    private String status;

    /**
     * 用户自定义状态: AVAILABLE, BUSY, AWAY, ASLEEP
     */
    @TableField("member_status")
    private String memberStatus;

    @TableField("joined_at")
    private LocalDateTime joinedAt;
}
