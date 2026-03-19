// Fixed service/AbstractHistoryService.java (made specific methods abstract to avoid undefined calls on generic JpaRepository)
package com.example.naim.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.example.naim.model.User;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class AbstractHistoryService<T, ID> {
    protected final JpaRepository<T, ID> repo;
    protected final RedisTemplate<String, Object> redis;
    protected final String prefix;
    protected final Duration ttl;

    public abstract T saveEntity(User user, String sessionId, Object... params);

    protected void cacheLast(String sessionId, String value) {
        try {
            redis.opsForValue().set(prefix + ":" + sessionId + ":last", value, ttl);
        } catch (Exception e) {
            // Logging handled in concrete classes
        }
    }

    public Optional<String> getLastFromCache(String sessionId) {
        try {
            Object v = redis.opsForValue().get(prefix + ":" + sessionId + ":last");
            return Optional.ofNullable(v != null ? v.toString() : null);
        } catch (Exception e) { return Optional.empty(); }
    }

    public abstract List<T> getHistory(User user, String sessionId);

    public abstract long getCount(User user, String sessionId);

    @Transactional
    public void clearHistory(User user, String sessionId) {
        try {
            String cacheKey = (user != null) ? user.getId().toString() : sessionId;
            redis.delete(prefix + ":" + cacheKey + ":last");
        } catch (Exception e) { }
        deleteByUserOrSession(user, sessionId); 
    }

    protected abstract void deleteByUserOrSession(User user, String sessionId);
}