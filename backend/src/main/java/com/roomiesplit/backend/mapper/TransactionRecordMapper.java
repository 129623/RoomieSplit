package com.roomiesplit.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.roomiesplit.backend.domain.TransactionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionRecordMapper extends BaseMapper<TransactionRecord> {
}
