package com.roomiesplit.backend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投票记录实体类
 */
@Data
@TableName("poll_vote")
public class PollVote {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("poll_id")
    private Long pollId;

    @TableField("user_id")
    private Long userId;

    @TableField("option_index")
    private Integer optionIndex;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
