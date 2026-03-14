// Updated service/QuizHistoryService.java (similar fixes)
package com.example.naim.service;

import com.example.naim.model.QuizHistory;
import com.example.naim.repository.QuizHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class QuizHistoryService extends AbstractHistoryService<QuizHistory, Long> {
    private static final String PREFIX = "quiz";
    private static final Duration TTL = Duration.ofHours(1);
    private static final Duration SCORE_TTL = Duration.ofHours(24);

    private final QuizHistoryRepository quizRepo;

    public QuizHistoryService(QuizHistoryRepository repo, RedisTemplate<String, Object> redis) {
        super(repo, redis, PREFIX, TTL);
        this.quizRepo = repo;
    }

    @Override
    public QuizHistory saveEntity(String sessionId, Object... params) {
        String sourceType = (String) params[0];
        String sourceRef = (String) params[1];
        String quizJson = (String) params[2];
        int totalQuestions = (int) params[3];
        QuizHistory entity = QuizHistory.builder()
            .sessionId(sessionId)
            .sourceType(sourceType)
            .sourceRef(sourceRef != null && sourceRef.length() > 500 ? sourceRef.substring(0, 500) : sourceRef)
            .quizJson(quizJson)
            .totalQuestions(totalQuestions)
            .build();
        try {
            entity = (QuizHistory) repo.save(entity);
        } catch (Exception e) {
            log.warn("MySQL saveQuiz failed: {}", e.getMessage());
        }
        cacheLast(sessionId, quizJson);
        return entity;
    }

    public void saveScore(Long quizId, String sessionId, int score) {
        try {
            quizRepo.findById(quizId).ifPresent(q -> {
                q.setScore(score);
                repo.save(q);
            });
        } catch (Exception e) {
            log.warn("saveScore MySQL: {}", e.getMessage());
        }
        try {
            redis.opsForValue().set(PREFIX + ":" + sessionId + ":score:" + quizId, String.valueOf(score), SCORE_TTL);
        } catch (Exception e) {
            log.warn("Redis saveScore: {}", e.getMessage());
        }
    }

    public Optional<QuizHistory> findCachedTopicQuiz(String sessionId, String topic) {
        try {
            List<QuizHistory> existing = quizRepo.findBySessionIdAndSourceTypeAndSourceRef(sessionId, "topic", topic);
            return existing.isEmpty() ? Optional.empty() : Optional.of(existing.get(0));
        } catch (Exception e) { return Optional.empty(); }
    }

    @Override
    public List<QuizHistory> getHistory(String sessionId) {
        try {
            return quizRepo.findBySessionIdOrderByCreatedAtDesc(sessionId);
        } catch (Exception e) {
            log.warn("getHistory failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long getCount(String sessionId) {
        try {
            return quizRepo.countBySessionId(sessionId);
        } catch (Exception e) {
            log.warn("getCount failed: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    protected void deleteBySessionId(String sessionId) {
        try {
            quizRepo.deleteBySessionId(sessionId);
        } catch (Exception e) {
            log.warn("deleteBySessionId failed: {}", e.getMessage());
        }
    }
}