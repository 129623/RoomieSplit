package com.roomiesplit.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.roomiesplit.backend.domain.Poll;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PollMapper extends BaseMapper<Poll> {
}
