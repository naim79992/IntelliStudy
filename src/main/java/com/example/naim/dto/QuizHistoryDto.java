// dto/QuizHistoryDto.java
package com.example.naim.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuizHistoryDto {
    private Long id;
    private String sourceType;
    private String sourceRef;
    private Integer totalQuestions;
    private Integer score;
    private LocalDateTime createdAt;
}