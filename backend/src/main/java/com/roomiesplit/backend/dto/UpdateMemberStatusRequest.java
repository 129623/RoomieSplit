package com.roomiesplit.backend.dto;

import lombok.Data;

@Data
public class UpdateMemberStatusRequest {
    private String status; // AVAILABLE, BUSY, AWAY, ASLEEP
}
