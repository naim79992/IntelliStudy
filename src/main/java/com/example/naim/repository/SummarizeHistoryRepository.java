// ─── SummarizeHistoryRepository.java ────────────────────────────────────────
package com.example.naim.repository;

import com.example.naim.model.SummarizeHistory;
import com.example.naim.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SummarizeHistoryRepository extends JpaRepository<SummarizeHistory, Long> {
    List<SummarizeHistory> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    void deleteBySessionId(String sessionId);
    long countBySessionId(String sessionId);

    List<SummarizeHistory> findByUserOrderByCreatedAtDesc(User user);
    void deleteByUser(User user);
    long countByUser(User user);
}

