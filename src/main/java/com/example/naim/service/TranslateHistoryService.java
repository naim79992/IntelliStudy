// Updated service/TranslateHistoryService.java (similar fixes)
package com.example.naim.service;

import com.example.naim.model.TranslateHistory;
import com.example.naim.repository.TranslateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class TranslateHistoryService extends AbstractHistoryService<TranslateHistory, Long> {
    private static final String PREFIX = "trans";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final TranslateHistoryRepository translateRepo;

    public TranslateHistoryService(TranslateHistoryRepository repo, RedisTemplate<String, Object> redis) {
        super(repo, redis, PREFIX, TTL);
        this.translateRepo = repo;
    }

    @Override
    public TranslateHistory saveEntity(String sessionId, Object... params) {
        String inputText = (String) params[0];
        String translatedText = (String) params[1];
        TranslateHistory entity = TranslateHistory.builder()
            .sessionId(sessionId)
            .inputText(inputText)
            .translatedText(translatedText)
            .sourceLang("en")
            .targetLang("bn")
            .build();
        try {
            entity = (TranslateHistory) repo.save(entity);
        } catch (Exception e) {
            log.warn("MySQL saveTranslation failed: {}", e.getMessage());
        }
        cacheLast(sessionId, translatedText);
        return entity;
    }

    @Override
    public List<TranslateHistory> getHistory(String sessionId) {
        try {
            return translateRepo.findBySessionIdOrderByCreatedAtDesc(sessionId);
        } catch (Exception e) {
            log.warn("getHistory failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long getCount(String sessionId) {
        try {
            return translateRepo.countBySessionId(sessionId);
        } catch (Exception e) {
            log.warn("getCount failed: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    protected void deleteBySessionId(String sessionId) {
        try {
            translateRepo.deleteBySessionId(sessionId);
        } catch (Exception e) {
            log.warn("deleteBySessionId failed: {}", e.getMessage());
        }
    }
}