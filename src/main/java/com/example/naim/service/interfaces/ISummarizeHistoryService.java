package com.example.naim.service.interfaces;

import com.example.naim.model.SummarizeHistory;
import com.example.naim.model.User;
import java.util.List;

public interface ISummarizeHistoryService {
    SummarizeHistory saveHistory(User user, String sessionId, String summary);
    List<SummarizeHistory> getHistory(User user, String sessionId);
    long getCount(User user, String sessionId);
    void clearHistory(User user, String sessionId);
}
