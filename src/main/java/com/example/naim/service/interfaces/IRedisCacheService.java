package com.example.naim.service.interfaces;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IRedisCacheService {
    // PDF Cache
    void cachePdfText(String sessionId, String text);
    Optional<String> getCachedPdfText(String sessionId);
    void cachePdfName(String sessionId, String fileName);
    Optional<String> getCachedPdfName(String sessionId);
    void setPdfLoaded(String sessionId, boolean loaded);
    boolean isPdfLoaded(String sessionId);
    void clearPdfCache(String sessionId);

    // Conversation Cache
    void appendMessage(String sessionId, String feature, String role, String content);
    List<Map<String, String>> getConversation(String sessionId, String feature);
    String buildHistoryBlock(String sessionId, String feature);
    boolean hasConversation(String sessionId, String feature);
    void clearConversation(String sessionId, String feature);

    // Session / Rate Limit
    void setSessionMeta(String sessionId, String field, String value);
    Optional<String> getSessionMeta(String sessionId, String field);
    void touchSession(String sessionId);
    boolean isAllowed(String sessionId, String feature);
    int getRemainingRequests(String sessionId, String feature);
    boolean isHealthy();
}
