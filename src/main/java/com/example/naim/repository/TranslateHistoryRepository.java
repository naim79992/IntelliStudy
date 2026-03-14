
// ─── TranslateHistoryRepository.java ────────────────────────────────────────
package com.example.naim.repository;

import com.example.naim.model.TranslateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TranslateHistoryRepository extends JpaRepository<TranslateHistory, Long> {
    List<TranslateHistory> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    void deleteBySessionId(String sessionId);
    long countBySessionId(String sessionId);
}
