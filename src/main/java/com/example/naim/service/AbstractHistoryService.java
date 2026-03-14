// Fixed service/AbstractHistoryService.java (made specific methods abstract to avoid undefined calls on generic JpaRepository)
package com.example.naim.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class AbstractHistoryService<T, ID> {
    protected final JpaRepository<T, ID> repo;
    protected final RedisTemplate<String, Object> redis;
    protected final String prefix;
    protected final Duration ttl;

    public abstract T saveEntity(String sessionId, Object... params);

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

    public abstract List<T> getHistory(String sessionId);

    public abstract long getCount(String sessionId);

    @Transactional
    public void clearHistory(String sessionId) {
        try {
            redis.delete(prefix + ":" + sessionId + ":last");
        } catch (Exception e) { }
        deleteBySessionId(sessionId); // Call abstract method for specific delete
    }

    protected abstract void deleteBySessionId(String sessionId);
}