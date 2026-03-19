package com.example.naim.service.impl;

import com.example.naim.dto.MessageDto;
import com.example.naim.enums.FeatureEnum;
import com.example.naim.model.ConversationMessage;
import com.example.naim.model.User;
import com.example.naim.repository.ConversationRepository;
import com.example.naim.service.interfaces.IRedisCacheService;
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
    private final IRedisCacheService redisCache;

    @Override
    public void saveMessage(User user, String sessionId, FeatureEnum feature, String role, String content) {
        String cacheKey = (user != null) ? user.getId().toString() : sessionId;
        redisCache.appendMessage(cacheKey, feature.getValue(), role, content);
        try {
            repo.save(ConversationMessage.builder()
                .user(user)
                .sessionId(sessionId).feature(feature.getValue())
                .role(role).content(content).build());
        } catch (Exception e) {
            log.warn("MySQL save failed (Redis still has it): {}", e.getMessage());
        }
        redisCache.touchSession(cacheKey);
    }

    @Override
    public String buildHistoryBlock(User user, String sessionId, FeatureEnum feature) {
        String cacheKey = (user != null) ? user.getId().toString() : sessionId;
        String cached = redisCache.buildHistoryBlock(cacheKey, feature.getValue());
        if (!cached.isBlank()) return cached;
        log.debug("Redis miss — loading from MySQL: user={} session={} feature={}", user, sessionId, feature.getValue());
        List<ConversationMessage> msgs;
        if (user != null) msgs = repo.findRecentByUser(user, feature.getValue(), HISTORY_LIMIT);
        else msgs = repo.findRecentMessages(sessionId, feature.getValue(), HISTORY_LIMIT);
        
        if (msgs.isEmpty()) return "";
        msgs.forEach(m -> redisCache.appendMessage(cacheKey, feature.getValue(), m.getRole(), m.getContent()));
        String lines = msgs.stream()
            .map(m -> capitalize(m.getRole()) + ": " + m.getContent())
            .collect(Collectors.joining("\n"));
        return "[CONVERSATION HISTORY]\n" + lines + "\n[END HISTORY]";
    }

    @Override
    public boolean hasHistory(User user, String sessionId, FeatureEnum feature) {
        String cacheKey = (user != null) ? user.getId().toString() : sessionId;
        if (redisCache.hasConversation(cacheKey, feature.getValue())) return true;
        try { 
            if (user != null) return repo.countByUserAndFeature(user, feature.getValue()) > 0;
            return repo.countBySessionIdAndFeature(sessionId, feature.getValue()) > 0; 
        }
        catch (Exception e) { return false; }
    }

    @Override
    public List<MessageDto> loadMessages(User user, String sessionId, FeatureEnum feature, int limit) {
        try {
            List<ConversationMessage> messages;
            if (user != null) messages = repo.findRecentByUser(user, feature.getValue(), limit);
            else messages = repo.findRecentMessages(sessionId, feature.getValue(), limit);
            
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
    public int getRemainingRequests(User user, String sessionId, FeatureEnum feature) {
        String cacheKey = (user != null) ? user.getId().toString() : sessionId;
        return redisCache.getRemainingRequests(cacheKey, feature.getValue());
    }

    @Override
    public boolean isAllowed(User user, String sessionId, FeatureEnum feature) {
        String cacheKey = (user != null) ? user.getId().toString() : sessionId;
        return redisCache.isAllowed(cacheKey, feature.getValue());
    }

    @Override
    public void clearFeature(User user, String sessionId, FeatureEnum feature) {
        String cacheKey = (user != null) ? user.getId().toString() : sessionId;
        redisCache.clearConversation(cacheKey, feature.getValue());
        try { 
            if (user != null) repo.deleteByUserAndFeature(user, feature.getValue());
            else repo.deleteBySessionIdAndFeature(sessionId, feature.getValue()); 
        }
        catch (Exception e) { log.warn("MySQL clear failed: {}", e.getMessage()); }
    }

    @Override
    public void clearAll(User user, String sessionId) {
        String cacheKey = (user != null) ? user.getId().toString() : sessionId;
        for (FeatureEnum f : FeatureEnum.values())
            redisCache.clearConversation(cacheKey, f.getValue());
        try { 
            if (user != null) repo.deleteByUser(user);
            else repo.deleteBySessionId(sessionId); 
        }
        catch (Exception e) { log.warn("MySQL clearAll failed: {}", e.getMessage()); }
    }

    private String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}