package com.roomiesplit.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.roomiesplit.backend.domain.PollVote;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PollVoteMapper extends BaseMapper<PollVote> {
}
