package com.example.naim.service.interfaces;

import com.example.naim.dto.MessageDto;
import com.example.naim.enums.FeatureEnum;

import java.util.List;

public interface ConversationInterface {
    void saveMessage(String sessionId, FeatureEnum feature, String role, String content);
    String buildHistoryBlock(String sessionId, FeatureEnum feature);
    boolean hasHistory(String sessionId, FeatureEnum feature);
    List<MessageDto> loadMessages(String sessionId, FeatureEnum feature, int limit);
    boolean isAllowed(String sessionId, FeatureEnum feature);
    int getRemainingRequests(String sessionId, FeatureEnum feature);
    void clearFeature(String sessionId, FeatureEnum feature);
    void clearAll(String sessionId);
}