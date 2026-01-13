package com.roomiesplit.backend.controller;

import com.roomiesplit.backend.common.Result;
import com.roomiesplit.backend.domain.SysUser;
import com.roomiesplit.backend.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @GetMapping("/users/{id}/profile")
    public Result<?> getUserProfile(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error(404, "User not found");
        }
        // Mask password
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/users/{id}/profile")
    public Result<?> updateUserProfile(@PathVariable Long id, @RequestBody java.util.Map<String, Object> request) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error(404, "User not found");
        }

        // Update avatarUrl if provided
        if (request.containsKey("avatarUrl")) {
            user.setAvatarUrl((String) request.get("avatarUrl"));
            sysUserService.updateById(user);
            return Result.success("Profile updated successfully");
        }

        return Result.error(400, "No valid fields to update");
    }
}
