package com.example.naim.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageDto {
    private String role;
    private String content;
    private LocalDateTime createdAt;
}