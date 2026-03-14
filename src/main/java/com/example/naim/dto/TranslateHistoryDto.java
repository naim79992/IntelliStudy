// dto/TranslateHistoryDto.java
package com.example.naim.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TranslateHistoryDto {
    private Long id;
    private String inputText;
    private String translatedText;
    private LocalDateTime createdAt;
}