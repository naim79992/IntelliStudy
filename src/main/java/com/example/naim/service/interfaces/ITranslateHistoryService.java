package com.example.naim.service.interfaces;

import com.example.naim.model.TranslateHistory;
import com.example.naim.model.User;
import java.util.List;

public interface ITranslateHistoryService {
    TranslateHistory saveHistory(User user, String sessionId, String translation);
    List<TranslateHistory> getHistory(User user, String sessionId);
    long getCount(User user, String sessionId);
    void clearHistory(User user, String sessionId);
}
