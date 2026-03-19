package com.example.naim.service.interfaces;

import com.example.naim.dto.MessageDto;
import com.example.naim.enums.FeatureEnum;
import com.example.naim.model.User;

import java.util.List;

public interface ConversationInterface {
    void saveMessage(User user, String sessionId, FeatureEnum feature, String role, String content);
    String buildHistoryBlock(User user, String sessionId, FeatureEnum feature);
    boolean hasHistory(User user, String sessionId, FeatureEnum feature);
    List<MessageDto> loadMessages(User user, String sessionId, FeatureEnum feature, int limit);
    boolean isAllowed(User user, String sessionId, FeatureEnum feature);
    int getRemainingRequests(User user, String sessionId, FeatureEnum feature);
    void clearFeature(User user, String sessionId, FeatureEnum feature);
    void clearAll(User user, String sessionId);
}