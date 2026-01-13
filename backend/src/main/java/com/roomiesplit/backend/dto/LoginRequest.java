package com.roomiesplit.backend.dto;

import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {
    private String email;
    private String password;
}
