package com.example.naim.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_history",
       indexes = @Index(name = "idx_quiz_session", columnList = "session_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    /** "topic" | "text" | "pdf" */
    @Column(name = "source_type", length = 16)
    private String sourceType;

    /** The topic string or first 500 chars of input text */
    @Column(name = "source_ref", columnDefinition = "TEXT")
    private String sourceRef;

    /** Full JSON array of generated questions */
    @Column(name = "quiz_json", nullable = false, columnDefinition = "LONGTEXT")
    private String quizJson;

    /** Score if the user submitted answers, null otherwise */
    @Column
    private Integer score;

    /** Total questions */
    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}