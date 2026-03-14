package com.example.naim.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SummarizeHistoryDto {
    private Long id;
    private String summary;
    private Integer charCount;
    private LocalDateTime createdAt;
}