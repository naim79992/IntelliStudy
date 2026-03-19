package com.example.naim.service.interfaces;

import com.example.naim.model.QuizHistory;
import com.example.naim.model.User;
import java.util.List;
import java.util.Optional;

public interface IQuizHistoryService {
    QuizHistory saveEntity(User user, String sessionId, Object... params);
    void saveHistory(User user, String sessionId, String quizJson);
    void saveScore(Long quizId, User user, String sessionId, int score);
    List<QuizHistory> getHistory(User user, String sessionId);
    long getCount(User user, String sessionId);
    void clearHistory(User user, String sessionId);
}
