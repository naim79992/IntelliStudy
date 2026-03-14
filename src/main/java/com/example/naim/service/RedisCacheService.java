package com.example.naim.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {
    private final RedisTemplate<String, Object> redis;
    private static final Duration CONV_TTL = Duration.ofHours(2);
    private static final Duration PDF_TTL = Duration.ofHours(6);
    private static final Duration SESSION_TTL = Duration.ofHours(1);
    private static final Duration RATE_TTL = Duration.ofMinutes(1);
    private static final int MAX_RPM = 20;

    private String convKey(String sid, String feature) {
        return "conv:" + sid + ":" + feature;
    }

    public void appendMessage(String sessionId, String feature, String role, String content) {
        try {
            String key = convKey(sessionId, feature);
            String val = role + "|" + content.replace("\n", "\\n");
            redis.opsForList().rightPush(key, val);
            redis.expire(key, CONV_TTL);
            Long size = redis.opsForList().size(key);
            if (size != null && size > 20) redis.opsForList().leftPop(key);
        } catch (Exception e) {
            log.warn("Redis appendMessage failed: {}", e.getMessage());
        }
    }

    public List<Map<String, String>> getConversation(String sessionId, String feature) {
        try {
            List<Object> raw = redis.opsForList().range(convKey(sessionId, feature), 0, -1);
            if (raw == null || raw.isEmpty()) return Collections.emptyList();
            List<Map<String, String>> result = new ArrayList<>();
            for (Object item : raw) {
                String str = item.toString();
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

    public boolean hasConversation(String sessionId, String feature) {
        try {
            Long s = redis.opsForList().size(convKey(sessionId, feature));
            return s != null && s > 0;
        } catch (Exception e) { return false; }
    }

    public void clearConversation(String sessionId, String feature) {
        try { redis.delete(convKey(sessionId, feature)); }
        catch (Exception e) { log.warn("Redis clearConv: {}", e.getMessage()); }
    }

    public void cachePdfText(String sessionId, String text) {
        try { redis.opsForValue().set("pdf:" + sessionId + ":text", text, PDF_TTL); }
        catch (Exception e) { log.warn("Redis op failed: {}", e.getMessage()); }
    }

    public Optional<String> getCachedPdfText(String sessionId) {
        try {
            Object v = redis.opsForValue().get("pdf:" + sessionId + ":text");
            return Optional.ofNullable(v != null ? v.toString() : null);
        } catch (Exception e) { return Optional.empty(); }
    }

    public void cachePdfName(String sessionId, String name) {
        try { redis.opsForValue().set("pdf:" + sessionId + ":name", name, PDF_TTL); }
        catch (Exception e) { log.warn("Redis op failed: {}", e.getMessage()); }
    }

    public Optional<String> getCachedPdfName(String sessionId) {
        try {
            Object v = redis.opsForValue().get("pdf:" + sessionId + ":name");
            return Optional.ofNullable(v != null ? v.toString() : null);
        } catch (Exception e) { return Optional.empty(); }
    }

    public void setPdfLoaded(String sessionId, boolean loaded) {
        try { redis.opsForValue().set("pdf:" + sessionId + ":loaded", String.valueOf(loaded), PDF_TTL); }
        catch (Exception e) { log.warn("Redis op failed: {}", e.getMessage()); }
    }

    public boolean isPdfLoaded(String sessionId) {
        try {
            Object v = redis.opsForValue().get("pdf:" + sessionId + ":loaded");
            return "true".equals(v != null ? v.toString() : null);
        } catch (Exception e) { return false; }
    }

    public void clearPdfCache(String sessionId) {
        try {
            redis.delete(List.of(
                "pdf:" + sessionId + ":text",
                "pdf:" + sessionId + ":name",
                "pdf:" + sessionId + ":loaded"
            ));
        } catch (Exception e) { log.warn("Redis clearPdf: {}", e.getMessage()); }
    }

    public void setSessionMeta(String sessionId, String field, String value) {
        try {
            String key = "session:" + sessionId;
            redis.opsForHash().put(key, field, value);
            redis.expire(key, SESSION_TTL);
        } catch (Exception e) { log.warn("Redis setMeta: {}", e.getMessage()); }
    }

    public Optional<String> getSessionMeta(String sessionId, String field) {
        try {
            Object v = redis.opsForHash().get("session:" + sessionId, field);
            return Optional.ofNullable(v != null ? v.toString() : null);
        } catch (Exception e) { return Optional.empty(); }
    }

    public void touchSession(String sessionId) {
        try {
            redis.expire("session:" + sessionId, SESSION_TTL);
            for (String f : List.of("ask", "rag", "quiz"))
                redis.expire(convKey(sessionId, f), CONV_TTL);
        } catch (Exception ignored) {}
    }

    public boolean isAllowed(String sessionId, String feature) {
        try {
            String key = "rate:" + sessionId + ":" + feature;
            Long count = redis.opsForValue().increment(key);
            if (count == null) return true;
            if (count == 1) redis.expire(key, RATE_TTL);
            return count <= MAX_RPM;
        } catch (Exception e) {
            log.warn("Redis rate limit check failed: {}", e.getMessage());
            return true;
        }
    }

    public int getRemainingRequests(String sessionId, String feature) {
        try {
            Object v = redis.opsForValue().get("rate:" + sessionId + ":" + feature);
            if (v == null) return MAX_RPM;
            return Math.max(0, MAX_RPM - Integer.parseInt(v.toString()));
        } catch (Exception e) { return MAX_RPM; }
    }

    public boolean isHealthy() {
        try { redis.opsForValue().get("__health__"); return true; }
        catch (Exception e) { return false; }
    }

    private String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s
            : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}