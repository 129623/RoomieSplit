package com.roomiesplit.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreatePollRequest {
    private String title;
    private List<String> options;
    private String mode; // VOTE, RANDOM
}
