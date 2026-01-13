package com.roomiesplit.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.roomiesplit.backend.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
