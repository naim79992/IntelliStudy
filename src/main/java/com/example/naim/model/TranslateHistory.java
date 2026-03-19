package com.example.naim.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Entity
@Table(name = "translate_history",
       indexes = @Index(name = "idx_trans_session", columnList = "session_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class TranslateHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_id", nullable = true, length = 128)
    private String sessionId;

    @Column(name = "source_lang", length = 16)
    private String sourceLang = "en";

    @Column(name = "target_lang", length = 16)
    private String targetLang = "bn";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String inputText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String translatedText;

}