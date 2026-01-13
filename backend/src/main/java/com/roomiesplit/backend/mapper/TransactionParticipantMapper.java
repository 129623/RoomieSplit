package com.roomiesplit.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.roomiesplit.backend.domain.TransactionParticipant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionParticipantMapper extends BaseMapper<TransactionParticipant> {
}
