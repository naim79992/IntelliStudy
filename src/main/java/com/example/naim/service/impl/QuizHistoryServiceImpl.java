package com.example.naim.service.impl;

import com.example.naim.model.QuizHistory;
import com.example.naim.model.User;
import com.example.naim.repository.QuizHistoryRepository;
import com.example.naim.service.AbstractHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.example.naim.service.interfaces.IQuizHistoryService;

@Slf4j
@Service
public class QuizHistoryServiceImpl extends AbstractHistoryService<QuizHistory, Long> implements IQuizHistoryService {
    private static final String PREFIX = "quiz";
    private static final Duration TTL = Duration.ofHours(1);
    private static final Duration SCORE_TTL = Duration.ofHours(24);

    private final QuizHistoryRepository quizRepo;

    public QuizHistoryServiceImpl(QuizHistoryRepository repo, RedisTemplate<String, Object> redis) {
        super(repo, redis, PREFIX, TTL);
        this.quizRepo = repo;
    }

    @Override
    public QuizHistory saveEntity(User user, String sessionId, Object... params) {
        String sourceType = (String) params[0];
        String sourceRef = (String) params[1];
        String quizJson = (String) params[2];
        int totalQuestions = (int) params[3];
        QuizHistory entity = QuizHistory.builder()
            .user(user)
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

    @Override
    public void saveHistory(User user, String sessionId, String quizJson) {
        saveEntity(user, sessionId, "unknown", "none", quizJson, 5);
    }

    @Override
    @Transactional
    public void saveScore(Long quizId, User user, String sessionId, int score) {
        try {
            quizRepo.findById(quizId).ifPresent(q -> {
                q.setScore(score);
                quizRepo.save(q);
            });
        } catch (Exception e) {
            log.warn("saveScore MySQL: {}", e.getMessage());
        }
        try {
            String cacheKey = (user != null) ? user.getId().toString() : sessionId;
            redis.opsForValue().set(PREFIX + ":" + cacheKey + ":score:" + quizId, String.valueOf(score), SCORE_TTL);
        } catch (Exception e) {
            log.warn("Redis saveScore: {}", e.getMessage());
        }
    }

    @Override
    public List<QuizHistory> getHistory(User user, String sessionId) {
        try {
            if (user != null) return quizRepo.findByUserOrderByCreatedAtDesc(user);
            return quizRepo.findBySessionIdOrderByCreatedAtDesc(sessionId);
        } catch (Exception e) {
            log.warn("getHistory failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long getCount(User user, String sessionId) {
        try {
            if (user != null) return quizRepo.countByUser(user);
            return quizRepo.countBySessionId(sessionId);
        } catch (Exception e) {
            log.warn("getCount failed: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    @Transactional
    protected void deleteByUserOrSession(User user, String sessionId) {
        try {
            if (user != null) quizRepo.deleteByUser(user);
            else quizRepo.deleteBySessionId(sessionId);
        } catch (Exception e) {
            log.warn("deleteBySessionId failed: {}", e.getMessage());
        }
    }
}