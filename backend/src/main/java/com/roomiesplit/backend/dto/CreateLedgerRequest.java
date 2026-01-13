package com.roomiesplit.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * 创建账本请求
 */
@Data
public class CreateLedgerRequest {
    private String name;
    private String description;
    private String defaultCurrency;
    private List<String> memberEmails; // 初始邀请成员邮箱
}
