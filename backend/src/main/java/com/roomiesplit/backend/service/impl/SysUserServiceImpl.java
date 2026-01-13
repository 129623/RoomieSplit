package com.roomiesplit.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.SysUser;
import com.roomiesplit.backend.dto.LoginRequest;
import com.roomiesplit.backend.dto.RegisterRequest;
import com.roomiesplit.backend.mapper.SysUserMapper;
import com.roomiesplit.backend.service.SysUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户服务实现类
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public Result<?> login(LoginRequest request) {
        if (!StringUtils.hasText(request.getEmail()) || !StringUtils.hasText(request.getPassword())) {
            return Result.error(400, "邮箱或密码不能为空");
        }

        // 查询用户
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getEmail, request.getEmail());
        SysUser user = this.getOne(queryWrapper);

        if (user == null) {
            return Result.error(401, "用户不存在");
        }

        // 验证密码 (简单MD5)
        String inputPasswordMd5 = DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8));
        if (!inputPasswordMd5.equals(user.getPassword())) {
            return Result.error(401, "密码错误");
        }

        // 生成Token (使用UUID模拟)
        String token = UUID.randomUUID().toString().replace("-", "");

        // 返回结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("token", token);
        resultData.put("user", safeUser(user));

        return Result.success("登录成功", resultData);
    }

    @Override
    public Result<?> register(RegisterRequest request) {
        if (!StringUtils.hasText(request.getEmail()) || !StringUtils.hasText(request.getPassword())) {
            return Result.error(400, "邮箱或密码不能为空");
        }

        // 检查邮箱是否已存在
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getEmail, request.getEmail());
        if (this.count(queryWrapper) > 0) {
            return Result.error(409, "该邮箱已被注册");
        }

        // 创建新用户
        SysUser newUser = new SysUser();
        newUser.setEmail(request.getEmail());
        newUser.setUsername(request.getEmail().split("@")[0]); // 默认用户名为邮箱前缀
        newUser.setDisplayName(StringUtils.hasText(request.getDisplayName()) ? request.getDisplayName() : "New User");
        // 简单MD5加密
        newUser.setPassword(DigestUtils.md5DigestAsHex(request.getPassword().getBytes(StandardCharsets.UTF_8)));
        newUser.setCreatedAt(LocalDateTime.now());

        this.save(newUser);

        // 生成Token
        String token = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("token", token);
        resultData.put("user", safeUser(newUser));

        return Result.success("注册成功", resultData);
    }

    /**
     * 脱敏用户数据
     * 
     * @param user 原始用户对象
     * @return 脱敏后的用户对象（不含密码）
     */
    private SysUser safeUser(SysUser user) {
        SysUser safe = new SysUser();
        safe.setId(user.getId());
        safe.setUsername(user.getUsername());
        safe.setEmail(user.getEmail());
        safe.setDisplayName(user.getDisplayName());
        safe.setAvatarUrl(user.getAvatarUrl());
        safe.setCreatedAt(user.getCreatedAt());
        // 不返回密码
        return safe;
    }
}
