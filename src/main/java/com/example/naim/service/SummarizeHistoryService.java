package com.example.naim.service;

import com.example.naim.model.SummarizeHistory;
import com.example.naim.repository.SummarizeHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class SummarizeHistoryService extends AbstractHistoryService<SummarizeHistory, Long> {
    private static final String PREFIX = "sum";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final SummarizeHistoryRepository summarizeRepo;

    public SummarizeHistoryService(SummarizeHistoryRepository repo, RedisTemplate<String, Object> redis) {
        super(repo, redis, PREFIX, TTL);
        this.summarizeRepo = repo;
    }

    @Override
    public SummarizeHistory saveEntity(String sessionId, Object... params) {
        String inputText = (String) params[0];
        String summary = (String) params[1];
        SummarizeHistory entity = SummarizeHistory.builder()
            .sessionId(sessionId)
            .inputText(inputText)
            .summary(summary)
            .build();
        try {
            entity = (SummarizeHistory) repo.save(entity);
        } catch (Exception e) {
            log.warn("MySQL saveSummary failed: {}", e.getMessage());
        }
        cacheLast(sessionId, summary);
        return entity;
    }

    @Override
    public List<SummarizeHistory> getHistory(String sessionId) {
        try {
            return summarizeRepo.findBySessionIdOrderByCreatedAtDesc(sessionId);
        } catch (Exception e) {
            log.warn("getHistory failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long getCount(String sessionId) {
        try {
            return summarizeRepo.countBySessionId(sessionId);
        } catch (Exception e) {
            log.warn("getCount failed: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    protected void deleteBySessionId(String sessionId) {
        try {
            summarizeRepo.deleteBySessionId(sessionId);
        } catch (Exception e) {
            log.warn("deleteBySessionId failed: {}", e.getMessage());
        }
    }
}