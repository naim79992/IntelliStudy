package com.example.naim.service.impl;

import com.example.naim.model.TranslateHistory;
import com.example.naim.model.User;
import com.example.naim.repository.TranslateHistoryRepository;
import com.example.naim.service.AbstractHistoryService;
import com.example.naim.service.interfaces.ITranslateHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class TranslateHistoryServiceImpl extends AbstractHistoryService<TranslateHistory, Long> implements ITranslateHistoryService {
    private static final String PREFIX = "translate";
    private static final Duration TTL = Duration.ofHours(1);

    private final TranslateHistoryRepository translateRepo;

    public TranslateHistoryServiceImpl(TranslateHistoryRepository repo, RedisTemplate<String, Object> redis) {
        super(repo, redis, PREFIX, TTL);
        this.translateRepo = repo;
    }

    @Override
    public TranslateHistory saveEntity(User user, String sessionId, Object... params) {
        String translation = (String) params[0];
        TranslateHistory entity = TranslateHistory.builder()
            .user(user)
            .sessionId(sessionId)
            .inputText("Document Translation") 
            .translatedText(translation)
            .build();
        try {
            entity = translateRepo.save(entity);
        } catch (Exception e) {
            log.warn("MySQL saveTranslate failed: {}", e.getMessage());
        }
        cacheLast(sessionId, translation);
        return entity;
    }

    @Override
    public TranslateHistory saveHistory(User user, String sessionId, String translation) {
        return saveEntity(user, sessionId, translation);
    }

    @Override
    public List<TranslateHistory> getHistory(User user, String sessionId) {
        try {
            if (user != null) return translateRepo.findByUserOrderByCreatedAtDesc(user);
            return translateRepo.findBySessionIdOrderByCreatedAtDesc(sessionId);
        } catch (Exception e) {
            log.warn("getHistory failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long getCount(User user, String sessionId) {
        try { 
            if (user != null) return translateRepo.countByUser(user);
            return translateRepo.countBySessionId(sessionId); 
        } catch (Exception e) { return 0; }
    }

    @Override
    @Transactional
    protected void deleteByUserOrSession(User user, String sessionId) {
        try { 
            if (user != null) translateRepo.deleteByUser(user);
            else translateRepo.deleteBySessionId(sessionId); 
        } catch (Exception e) {}
    }
}