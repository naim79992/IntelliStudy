package com.example.naim.service.impl;

import com.example.naim.model.SummarizeHistory;
import com.example.naim.model.User;
import com.example.naim.repository.SummarizeHistoryRepository;
import com.example.naim.service.AbstractHistoryService;
import com.example.naim.service.interfaces.ISummarizeHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class SummarizeHistoryServiceImpl extends AbstractHistoryService<SummarizeHistory, Long> implements ISummarizeHistoryService {
    private static final String PREFIX = "summarize";
    private static final Duration TTL = Duration.ofHours(1);

    private final SummarizeHistoryRepository summarizeRepo;

    public SummarizeHistoryServiceImpl(SummarizeHistoryRepository repo, RedisTemplate<String, Object> redis) {
        super(repo, redis, PREFIX, TTL);
        this.summarizeRepo = repo;
    }

    @Override
    public SummarizeHistory saveEntity(User user, String sessionId, Object... params) {
        String summary = (String) params[0]; 
        SummarizeHistory entity = SummarizeHistory.builder()
            .user(user)
            .sessionId(sessionId)
            .inputText("PDF Document Summary") 
            .summary(summary)
            .build();
        try {
            entity = summarizeRepo.save(entity);
        } catch (Exception e) {
            log.warn("MySQL saveSummarize failed: {}", e.getMessage());
        }
        cacheLast(sessionId, summary);
        return entity;
    }

    @Override
    public SummarizeHistory saveHistory(User user, String sessionId, String summary) {
        return saveEntity(user, sessionId, summary);
    }

    @Override
    public List<SummarizeHistory> getHistory(User user, String sessionId) {
        try {
            if (user != null) return summarizeRepo.findByUserOrderByCreatedAtDesc(user);
            return summarizeRepo.findBySessionIdOrderByCreatedAtDesc(sessionId);
        } catch (Exception e) {
            log.warn("getHistory failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long getCount(User user, String sessionId) {
        try {
            if (user != null) return summarizeRepo.countByUser(user);
            return summarizeRepo.countBySessionId(sessionId);
        } catch (Exception e) { return 0; }
    }

    @Override
    @Transactional
    protected void deleteByUserOrSession(User user, String sessionId) {
        try { 
            if (user != null) summarizeRepo.deleteByUser(user);
            else summarizeRepo.deleteBySessionId(sessionId); 
        } catch (Exception e) {}
    }
}