package com.roomiesplit.backend.dto;

import lombok.Data;

@Data
public class KarmaWorkRequest {
    private String category;
    private Integer points;
    private String description;
}
