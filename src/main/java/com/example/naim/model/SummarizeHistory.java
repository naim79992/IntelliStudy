package com.example.naim.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Entity
@Table(name = "summarize_history",
       indexes = @Index(name = "idx_sum_session", columnList = "session_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class SummarizeHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_id", nullable = true, length = 128)
    private String sessionId;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String inputText;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String summary;

    @Column(name = "char_count")
    private Integer charCount;

    @PrePersist
    protected void onCreate() {
        if (charCount == null && inputText != null) charCount = inputText.length();
    }
}