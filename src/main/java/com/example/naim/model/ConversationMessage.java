package com.example.naim.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Stores every message in a conversation session.
 *
 * session_id  → HTTP session (identifies the user/browser)
 * feature     → "ask" | "rag" | "quiz"
 * role        → "user" | "assistant"
 * content     → the actual message text
 * created_at  → for ordering and cleanup
 */
@Entity
@Table(name = "conversation_messages",
       indexes = {
           @Index(name = "idx_session_feature", columnList = "session_id, feature"),
           @Index(name = "idx_created_at",      columnList = "created_at")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    /**
     * Which feature this message belongs to:
     *  "ask"  – General Q&A
     *  "rag"  – PDF Chat
     *  "quiz" – Quiz generation history
     */
    @Column(nullable = false, length = 16)
    private String feature;

    /** "user" or "assistant" */
    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}