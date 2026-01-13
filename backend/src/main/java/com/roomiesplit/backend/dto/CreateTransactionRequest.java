package com.roomiesplit.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建交易请求
 */
@Data
public class CreateTransactionRequest {
    private Long ledgerId;
    private BigDecimal amount;
    private String category;
    private String description;
    private LocalDateTime date;
    private String splitType; // EQUAL, EXACT
    private Long payerId;

    /**
     * 参与者列表 (包含用户ID和应付金额/权重)
     */
    private List<ReqParticipant> participants;

    @Data
    public static class ReqParticipant {
        private Long userId;
        private BigDecimal amount; // if EXACT
        // private Double weight; // if WEIGHTED (暂不实现复杂权重)
    }
}
