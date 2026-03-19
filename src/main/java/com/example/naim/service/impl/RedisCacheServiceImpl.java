package com.example.naim.service.impl;

import com.example.naim.service.interfaces.IRedisCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements IRedisCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheServiceImpl.class);
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final Duration CONV_TTL = Duration.ofHours(2);
    private static final Duration PDF_TTL  = Duration.ofDays(7);
    private static final Duration SESSION_TTL = Duration.ofHours(1);
    private static final Duration RATE_TTL = Duration.ofMinutes(1);
    private static final int MAX_RPM = 20;

    private String convKey(String sid, String feature) {
        return "conv:" + sid + ":" + feature;
    }

    @Override
    public void appendMessage(String sessionId, String feature, String role, String content) {
        try {
            String key = convKey(sessionId, feature);
            String val = role + "|" + content.replace("\n", "\\n");
            redisTemplate.opsForList().rightPush(key, val);
            redisTemplate.expire(key, CONV_TTL);
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > 20) redisTemplate.opsForList().leftPop(key);
        } catch (Exception e) {
            log.warn("Redis appendMessage failed: {}", e.getMessage());
        }
    }

    @Override
    public List<Map<String, String>> getConversation(String sessionId, String feature) {
        try {
            List<String> raw = redisTemplate.opsForList().range(convKey(sessionId, feature), 0, -1);
            if (raw == null || raw.isEmpty()) return Collections.emptyList();
            List<Map<String, String>> result = new ArrayList<>();
            for (String str : raw) {
                int sep = str.indexOf('|');
                if (sep > 0) {
                    result.add(Map.of(
                        "role", str.substring(0, sep),
                        "content", str.substring(sep + 1).replace("\\n", "\n")
                    ));
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Redis getConversation failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String buildHistoryBlock(String sessionId, String feature) {
        List<Map<String, String>> msgs = getConversation(sessionId, feature);
        if (msgs.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("[CONVERSATION HISTORY]\n");
        for (Map<String, String> m : msgs) {
            sb.append(capitalize(m.get("role")))
              .append(": ")
              .append(m.get("content"))
              .append("\n");
        }
        sb.append("[END HISTORY]");
        return sb.toString();
    }

    @Override
    public boolean hasConversation(String sessionId, String feature) {
        try {
            Long s = redisTemplate.opsForList().size(convKey(sessionId, feature));
            return s != null && s > 0;
        } catch (Exception e) { return false; }
    }

    @Override
    public void clearConversation(String sessionId, String feature) {
        try { redisTemplate.delete(convKey(sessionId, feature)); }
        catch (Exception e) { log.warn("Redis clearConv: {}", e.getMessage()); }
    }

    @Override
    public void cachePdfText(String sessionId, String text) {
        try { redisTemplate.opsForValue().set("pdf:" + sessionId + ":text", text, PDF_TTL); }
        catch (Exception e) { log.warn("Redis cachePdfText failed: {}", e.getMessage()); }
    }

    @Override
    public Optional<String> getCachedPdfText(String sessionId) {
        try {
            String v = redisTemplate.opsForValue().get("pdf:" + sessionId + ":text");
            return Optional.ofNullable(v);
        } catch (Exception e) { return Optional.empty(); }
    }

    @Override
    public void cachePdfName(String sessionId, String name) {
        try { redisTemplate.opsForValue().set("pdf:" + sessionId + ":name", name, PDF_TTL); }
        catch (Exception e) { log.warn("Redis cachePdfName failed: {}", e.getMessage()); }
    }

    @Override
    public Optional<String> getCachedPdfName(String sessionId) {
        try {
            String v = redisTemplate.opsForValue().get("pdf:" + sessionId + ":name");
            return Optional.ofNullable(v);
        } catch (Exception e) { return Optional.empty(); }
    }

    @Override
    public void setPdfLoaded(String sessionId, boolean loaded) {
        try { redisTemplate.opsForValue().set("pdf:" + sessionId + ":loaded", String.valueOf(loaded), PDF_TTL); }
        catch (Exception e) { log.warn("Redis setPdfLoaded failed: {}", e.getMessage()); }
    }

    @Override
    public boolean isPdfLoaded(String sessionId) {
        try {
            String v = redisTemplate.opsForValue().get("pdf:" + sessionId + ":loaded");
            return "true".equals(v);
        } catch (Exception e) { return false; }
    }

    @Override
    public void clearPdfCache(String sessionId) {
        try {
            redisTemplate.delete(List.of(
                "pdf:" + sessionId + ":text",
                "pdf:" + sessionId + ":name",
                "pdf:" + sessionId + ":loaded"
            ));
        } catch (Exception e) { log.warn("Redis clearPdfCache failed: {}", e.getMessage()); }
    }

    @Override
    public void setSessionMeta(String sessionId, String field, String value) {
        try {
            String key = "session:" + sessionId;
            redisTemplate.opsForHash().put(key, field, value);
            redisTemplate.expire(key, SESSION_TTL);
        } catch (Exception e) { log.warn("Redis setSessionMeta failed: {}", e.getMessage()); }
    }

    @Override
    public Optional<String> getSessionMeta(String sessionId, String field) {
        try {
            Object v = redisTemplate.opsForHash().get("session:" + sessionId, field);
            return Optional.ofNullable(v != null ? v.toString() : null);
        } catch (Exception e) { return Optional.empty(); }
    }

    @Override
    public void touchSession(String sessionId) {
        try {
            redisTemplate.expire("session:" + sessionId, SESSION_TTL);
            for (String f : List.of("ask", "rag", "quiz"))
                redisTemplate.expire(convKey(sessionId, f), CONV_TTL);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isAllowed(String sessionId, String feature) {
        try {
            String key = "rate:" + sessionId + ":" + feature;
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) return true;
            if (count == 1) redisTemplate.expire(key, RATE_TTL);
            return count <= MAX_RPM;
        } catch (Exception e) {
            log.warn("Redis rate limit check failed: {}", e.getMessage());
            return true;
        }
    }

    @Override
    public int getRemainingRequests(String sessionId, String feature) {
        try {
            String v = redisTemplate.opsForValue().get("rate:" + sessionId + ":" + feature);
            if (v == null) return MAX_RPM;
            return Math.max(0, MAX_RPM - Integer.parseInt(v));
        } catch (Exception e) { return MAX_RPM; }
    }

    @Override
    public boolean isHealthy() {
        try { redisTemplate.opsForValue().get("__health__"); return true; }
        catch (Exception e) { return false; }
    }

    private String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s
            : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}