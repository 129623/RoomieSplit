package com.roomiesplit.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.SysUser;
import com.roomiesplit.backend.dto.LoginRequest;
import com.roomiesplit.backend.dto.RegisterRequest;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户登录
     * @param request 登录请求参数
     * @return 登录结果
     */
    Result<?> login(LoginRequest request);

    /**
     * 用户注册
     * @param request 注册请求参数
     * @return 注册结果
     */
    Result<?> register(RegisterRequest request);
}
