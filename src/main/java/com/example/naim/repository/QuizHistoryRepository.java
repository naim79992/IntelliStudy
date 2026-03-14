package com.example.naim.repository;
import com.example.naim.model.QuizHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {
    List<QuizHistory> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    void deleteBySessionId(String sessionId);
    long countBySessionId(String sessionId);
    List<QuizHistory> findBySessionIdAndSourceTypeAndSourceRef(
        String sessionId, String sourceType, String sourceRef);
}