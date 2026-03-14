package com.example.naim.repository;

import com.example.naim.model.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationMessage, Long> {

    /**
     * Fetch the most recent N messages for a session + feature, oldest-first
     * (so they read naturally as a conversation).
     */
    @Query(value = """
        SELECT * FROM (
            SELECT * FROM conversation_messages
            WHERE session_id = :sessionId AND feature = :feature
            ORDER BY created_at DESC
            LIMIT :limit
        ) sub ORDER BY created_at ASC
        """, nativeQuery = true)
    List<ConversationMessage> findRecentMessages(
        @Param("sessionId") String sessionId,
        @Param("feature")   String feature,
        @Param("limit")     int limit
    );

    /** Count messages in a session/feature (used for stats). */
    long countBySessionIdAndFeature(String sessionId, String feature);

    /** Clear all messages for a session+feature (e.g. when PDF is removed). */
    @Modifying
    @Transactional
    @Query("DELETE FROM ConversationMessage m WHERE m.sessionId = :sessionId AND m.feature = :feature")
    void deleteBySessionIdAndFeature(
        @Param("sessionId") String sessionId,
        @Param("feature")   String feature
    );

    /** Clear entire session (all features). */
    @Modifying
    @Transactional
    void deleteBySessionId(String sessionId);
}