// ─── SummarizeHistoryRepository.java ────────────────────────────────────────
package com.example.naim.repository;

import com.example.naim.model.SummarizeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SummarizeHistoryRepository extends JpaRepository<SummarizeHistory, Long> {
    List<SummarizeHistory> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    void deleteBySessionId(String sessionId);
    long countBySessionId(String sessionId);
}

