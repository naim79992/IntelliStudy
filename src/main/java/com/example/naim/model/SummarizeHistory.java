package com.example.naim.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "summarize_history",
       indexes = @Index(name = "idx_sum_session", columnList = "session_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SummarizeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String inputText;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String summary;

    @Column(name = "char_count")
    private Integer charCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (charCount == null && inputText != null) charCount = inputText.length();
    }
}