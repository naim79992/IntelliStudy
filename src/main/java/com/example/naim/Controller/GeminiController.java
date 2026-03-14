package com.example.naim.Controller;

import com.example.naim.dto.ApiResponse;
// import com.example.naim.dto.QuizHistoryDto;
// import com.example.naim.dto.SummarizeHistoryDto;
// import com.example.naim.dto.TranslateHistoryDto;
import com.example.naim.enums.FeatureEnum;
import com.example.naim.model.QuizHistory;
import com.example.naim.model.SummarizeHistory;
import com.example.naim.model.TranslateHistory;
import com.example.naim.service.QuizHistoryService;
import com.example.naim.service.SummarizeHistoryService;
import com.example.naim.service.TranslateHistoryService;
import com.example.naim.service.interfaces.AiService;
import com.example.naim.service.interfaces.ConversationInterface;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {
    private final AiService aiService;
    private final ConversationInterface conversationService;
    private final SummarizeHistoryService summarizeHistoryService;
    private final TranslateHistoryService translateHistoryService;
    private final QuizHistoryService quizHistoryService;

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<String>> ask(@RequestBody String prompt, HttpSession session) {
        if (prompt == null || prompt.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                .success(false)
                .message("Prompt cannot be empty.")
                .build());
        String sid = session.getId();
        if (!conversationService.isAllowed(sid, FeatureEnum.ASK))
            return ResponseEntity.status(429).body(ApiResponse.<String>builder()
                .success(false)
                .message("Too many requests. Please wait.")
                .build());
        conversationService.saveMessage(sid, FeatureEnum.ASK, "user", prompt.trim());
        String history = conversationService.buildHistoryBlock(sid, FeatureEnum.ASK);
        String answer = aiService.generateResponse( """
            You are IntelliStudy, a helpful and friendly AI learning assistant.
            You remember previous messages in this conversation and use them for context.
            %s
            Current question: %s
            Answer clearly and helpfully. If the question refers to something from earlier
            in the conversation, use that context in your answer.
            """.formatted(history.isBlank() ? "" : history, prompt.trim()));
        conversationService.saveMessage(sid, FeatureEnum.ASK, "assistant", answer);
        return ResponseEntity.ok(ApiResponse.<String>builder()
            .success(true)
            .data(answer)
            .build());
    }

    @PostMapping("/summarize")
    public ResponseEntity<ApiResponse<String>> summarize(@RequestBody String text, HttpSession session) {
        if (text == null || text.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                .success(false)
                .message("Text cannot be empty.")
                .build());
        String sid = session.getId();
        String summary = aiService.summarizeText(text);
        summarizeHistoryService.saveEntity(sid, text, summary);
        return ResponseEntity.ok(ApiResponse.<String>builder()
            .success(true)
            .data(summary)
            .build());
    }

    @PostMapping("/translate")
    public ResponseEntity<ApiResponse<String>> translate(@RequestBody String text, HttpSession session) {
        if (text == null || text.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                .success(false)
                .message("Text cannot be empty.")
                .build());
        String sid = session.getId();
        String translated = aiService.translateToBangla(text);
        translateHistoryService.saveEntity(sid, text, translated);
        return ResponseEntity.ok(ApiResponse.<String>builder()
            .success(true)
            .data(translated)
            .build());
    }

    @PostMapping("/quiz/topic-json")
    public ResponseEntity<ApiResponse<String>> quizByTopic(@RequestBody String topic, HttpSession session) {
        if (topic == null || topic.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                .success(false)
                .data("[]")
                .build());
        String sid = session.getId();
        Optional<QuizHistory> cached = quizHistoryService.findCachedTopicQuiz(sid, topic.trim());
        if (cached.isPresent()) {
            return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .data(cached.get().getQuizJson())
                .build());
        }
        conversationService.saveMessage(sid, FeatureEnum.QUIZ, "user", "Generate quiz about: " + topic.trim());
        String history = conversationService.buildHistoryBlock(sid, FeatureEnum.QUIZ);
        String quiz = aiService.generateQuizJsonWithHistory(history, topic.trim());
        int total = countQuestions(quiz);
        quizHistoryService.saveEntity(sid, "topic", topic.trim(), quiz, total);
        conversationService.saveMessage(sid, FeatureEnum.QUIZ, "assistant", "Generated " + total + "-question quiz about: " + topic.trim());
        return ResponseEntity.ok(ApiResponse.<String>builder()
            .success(true)
            .data(quiz)
            .build());
    }

    @PostMapping("/quiz/text-json")
    public ResponseEntity<ApiResponse<String>> quizFromText(@RequestBody String text, HttpSession session) {
        if (text == null || text.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                .success(false)
                .data("[]")
                .build());
        String sid = session.getId();
        String quiz = aiService.generateQuizFromText(text);
        int total = countQuestions(quiz);
        quizHistoryService.saveEntity(sid, "text", text.substring(0, Math.min(500, text.length())), quiz, total);
        return ResponseEntity.ok(ApiResponse.<String>builder()
            .success(true)
            .data(quiz)
            .build());
    }

    @PostMapping("/quiz/score")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveScore(
            @RequestBody Map<String, Object> body, HttpSession session) {
        try {
            Long quizId = Long.valueOf(body.get("quizId").toString());
            int score = Integer.parseInt(body.get("score").toString());
            quizHistoryService.saveScore(quizId, session.getId(), score);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(Map.of("success", true))
                .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .data(Map.of("success", false, "message", e.getMessage()))
                .build());
        }
    }

   @GetMapping("/history/summarize")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summarizeHistory(HttpSession session) {
        List<SummarizeHistory> list = summarizeHistoryService.getHistory(session.getId());
        List<Map<String, Object>> historyList = list.stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("summary", h.getSummary());
            map.put("charCount", h.getCharCount());
            map.put("createdAt", h.getCreatedAt().toString());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
            .success(true)
            .data(Map.of(
                "count", list.size(),
                "history", historyList
            ))
            .build());
    }

    @GetMapping("/history/translate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> translateHistory(HttpSession session) {
        List<TranslateHistory> list = translateHistoryService.getHistory(session.getId());
        List<Map<String, Object>> historyList = list.stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("inputText", h.getInputText());
            map.put("translatedText", h.getTranslatedText());
            map.put("createdAt", h.getCreatedAt().toString());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
            .success(true)
            .data(Map.of(
                "count", list.size(),
                "history", historyList
            ))
            .build());
    }

    @GetMapping("/history/quiz")
    public ResponseEntity<ApiResponse<Map<String, Object>>> quizHistory(HttpSession session) {
        List<QuizHistory> list = quizHistoryService.getHistory(session.getId());
        List<Map<String, Object>> historyList = list.stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("sourceType", h.getSourceType());
            map.put("sourceRef", h.getSourceRef() != null ? h.getSourceRef() : "");
            map.put("totalQuestions", h.getTotalQuestions() != null ? h.getTotalQuestions() : 0);
            map.put("score", h.getScore() != null ? h.getScore() : -1);
            map.put("createdAt", h.getCreatedAt().toString());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
            .success(true)
            .data(Map.of(
                "count", list.size(),
                "history", historyList
            ))
            .build());
    }

    @GetMapping("/history/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats(HttpSession session) {
        String sid = session.getId();
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
            .success(true)
            .data(Map.of(
                "summarizeCount", summarizeHistoryService.getCount(sid),
                "translateCount", translateHistoryService.getCount(sid),
                "quizCount", quizHistoryService.getCount(sid),
                "hasAskHistory", conversationService.hasHistory(sid, FeatureEnum.ASK),
                "remainingAsk", conversationService.getRemainingRequests(sid, FeatureEnum.ASK)
            ))
            .build());
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Object>> clearHistory(
            @RequestParam(defaultValue = "ask") String featureStr, HttpSession session) {
        String sid = session.getId();
        FeatureEnum feature;
        try {
            feature = FeatureEnum.valueOf(featureStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                .success(false)
                .message("Unknown feature: " + featureStr)
                .build());
        }
        switch (feature) {
            case ASK -> conversationService.clearFeature(sid, FeatureEnum.ASK);
            case RAG -> conversationService.clearFeature(sid, FeatureEnum.RAG);
            case QUIZ -> {
                quizHistoryService.clearHistory(sid);
                conversationService.clearFeature(sid, FeatureEnum.QUIZ);
            }
            // Add for summarize and translate
            default -> { return ResponseEntity.badRequest().body(ApiResponse.builder()
                .success(false)
                .message("Unsupported feature for clear")
                .build()); }
        }
        return ResponseEntity.ok(ApiResponse.builder()
            .success(true)
            .message(feature.getValue() + " history cleared.")
            .build());
    }

    @GetMapping("/history/count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> historyCount(
            @RequestParam(defaultValue = "ask") String featureStr, HttpSession session) {
        FeatureEnum feature = FeatureEnum.valueOf(featureStr.toUpperCase());
        String sid = session.getId();
        boolean has = conversationService.hasHistory(sid, feature);
        int remaining = conversationService.getRemainingRequests(sid, feature);
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
            .success(true)
            .data(Map.of(
                "hasHistory", has,
                "remainingRequests", remaining
            ))
            .build());
    }

    private int countQuestions(String quizJson) {
        try {
            int count = 0, idx = 0;
            while ((idx = quizJson.indexOf("\"question\"", idx)) != -1) { count++; idx += 10; }
            return count;
        } catch (Exception e) { return 5; }
    }
}