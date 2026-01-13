package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.dto.LoginRequest;
import com.roomiesplit.backend.dto.RegisterRequest;
import com.roomiesplit.backend.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // 允许跨域
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 登录接口
     *
     * @param request 登录请求体，包含邮箱和密码
     * @return 登录结果，包含 Token 和用户信息
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest request) {
        return sysUserService.login(request);
    }

    /**
     * 注册接口
     *
     * @param request 注册请求体，包含邮箱、密码和昵称
     * @return 注册结果，包含 Token 和用户信息
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterRequest request) {
        return sysUserService.register(request);
    }
}
