package com.roomiesplit.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.roomiesplit.backend.domain.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
