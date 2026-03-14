package com.example.naim.service.impl;

import com.example.naim.dto.MessageDto;
import com.example.naim.enums.FeatureEnum;
import com.example.naim.model.ConversationMessage;
import com.example.naim.repository.ConversationRepository;
import com.example.naim.service.RedisCacheService;
import com.example.naim.service.interfaces.ConversationInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationInterface {
    private static final int HISTORY_LIMIT = 10;
    private final ConversationRepository repo;
    private final RedisCacheService redisCache;

    @Override
    public void saveMessage(String sessionId, FeatureEnum feature, String role, String content) {
        redisCache.appendMessage(sessionId, feature.getValue(), role, content);
        try {
            repo.save(ConversationMessage.builder()
                .sessionId(sessionId).feature(feature.getValue())
                .role(role).content(content).build());
        } catch (Exception e) {
            log.warn("MySQL save failed (Redis still has it): {}", e.getMessage());
        }
        redisCache.touchSession(sessionId);
    }

    @Override
    public String buildHistoryBlock(String sessionId, FeatureEnum feature) {
        String cached = redisCache.buildHistoryBlock(sessionId, feature.getValue());
        if (!cached.isBlank()) return cached;
        log.debug("Redis miss — loading from MySQL: session={} feature={}", sessionId, feature.getValue());
        List<ConversationMessage> msgs = repo.findRecentMessages(sessionId, feature.getValue(), HISTORY_LIMIT);
        if (msgs.isEmpty()) return "";
        msgs.forEach(m -> redisCache.appendMessage(sessionId, feature.getValue(), m.getRole(), m.getContent()));
        String lines = msgs.stream()
            .map(m -> capitalize(m.getRole()) + ": " + m.getContent())
            .collect(Collectors.joining("\n"));
        return "[CONVERSATION HISTORY]\n" + lines + "\n[END HISTORY]";
    }

    @Override
    public boolean hasHistory(String sessionId, FeatureEnum feature) {
        if (redisCache.hasConversation(sessionId, feature.getValue())) return true;
        try { return repo.countBySessionIdAndFeature(sessionId, feature.getValue()) > 0; }
        catch (Exception e) { return false; }
    }

    @Override
    public List<MessageDto> loadMessages(String sessionId, FeatureEnum feature, int limit) {
        try {
            List<ConversationMessage> messages = repo.findRecentMessages(sessionId, feature.getValue(), limit);
            return messages.stream().map(m -> {
                MessageDto dto = new MessageDto();
                dto.setRole(m.getRole());
                dto.setContent(m.getContent());
                dto.setCreatedAt(m.getCreatedAt());
                return dto;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("MySQL load failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean isAllowed(String sessionId, FeatureEnum feature) {
        return redisCache.isAllowed(sessionId, feature.getValue());
    }

    @Override
    public int getRemainingRequests(String sessionId, FeatureEnum feature) {
        return redisCache.getRemainingRequests(sessionId, feature.getValue());
    }

    @Override
    public void clearFeature(String sessionId, FeatureEnum feature) {
        redisCache.clearConversation(sessionId, feature.getValue());
        try { repo.deleteBySessionIdAndFeature(sessionId, feature.getValue()); }
        catch (Exception e) { log.warn("MySQL clear failed: {}", e.getMessage()); }
    }

    @Override
    public void clearAll(String sessionId) {
        for (FeatureEnum f : FeatureEnum.values())
            redisCache.clearConversation(sessionId, f.getValue());
        try { repo.deleteBySessionId(sessionId); }
        catch (Exception e) { log.warn("MySQL clearAll failed: {}", e.getMessage()); }
    }

    private String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}