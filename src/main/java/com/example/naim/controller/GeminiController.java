package com.example.naim.controller;

import com.example.naim.dto.ApiResponse;
import com.example.naim.service.interfaces.IRagService;
import com.example.naim.service.interfaces.IRedisCacheService;
import com.example.naim.service.interfaces.AiService;
import com.example.naim.model.User;
import com.example.naim.enums.FeatureEnum;
import com.example.naim.service.interfaces.IQuizHistoryService;
import com.example.naim.service.interfaces.ISummarizeHistoryService;
import com.example.naim.service.interfaces.ITranslateHistoryService;
import com.example.naim.service.interfaces.ConversationInterface;
import com.example.naim.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {

    private final AiService                  aiService;
    private final IRagService                ragService;
    private final IRedisCacheService         redisCache;
    private final IQuizHistoryService        quizHistory;
    private final ISummarizeHistoryService   summarizeHistory;
    private final ITranslateHistoryService   translateHistory;
    private final ConversationInterface      conversationService;

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<String>> ask(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = (principal != null) ? principal.getUser() : null;
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : request.getOrDefault("sessionId", "guest");
        
        String q = request.get("question");
        if (q == null || q.isBlank()) return ResponseEntity.badRequest().body(ApiResponse.<String>builder().success(false).message("No question").build());
        
        String context = ragService.getRelevantContext(sessionId, q);
        String history = conversationService.buildHistoryBlock(user, sessionId, FeatureEnum.ASK);
        
        String ans;
        if (context == null || context.isBlank()) {
            ans = aiService.generateResponseWithHistory(history, q);
        } else {
            ans = aiService.askWithContextAndHistory(context, history, q);
        }

        conversationService.saveMessage(user, sessionId, FeatureEnum.ASK, "user", q);
        conversationService.saveMessage(user, sessionId, FeatureEnum.ASK, "assistant", ans);
        
        return ResponseEntity.ok(ApiResponse.<String>builder().success(true).data(ans).build());
    }

    @PostMapping("/summarize")
    public ResponseEntity<ApiResponse<String>> summarize(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = (principal != null) ? principal.getUser() : null;
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : request.getOrDefault("sessionId", "guest");
        String text = request.get("text");
        String finalContent = (text != null && !text.isBlank()) ? text : ragService.getFullText(sessionId);
        
        if (finalContent.isBlank()) return ResponseEntity.badRequest().body(ApiResponse.<String>builder().success(false).message("No content").build());
        
        String res = aiService.summarizePdf(finalContent);
        summarizeHistory.saveHistory(user, sessionId, res);
        return ResponseEntity.ok(ApiResponse.<String>builder().success(true).data(res).build());
    }

    @PostMapping("/quiz")
    public ResponseEntity<ApiResponse<String>> quiz(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = (principal != null) ? principal.getUser() : null;
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : request.getOrDefault("sessionId", "guest");
        String topic = request.get("topic");
        String textFromReq = request.get("text");
        String res;

        if (topic != null && !topic.isBlank()) {
            String history = conversationService.buildHistoryBlock(user, sessionId, FeatureEnum.QUIZ);
            res = aiService.generateQuizJsonWithHistory(history, topic);
            conversationService.saveMessage(user, sessionId, FeatureEnum.QUIZ, "user", "Quiz about: " + topic);
        } else if (textFromReq != null && !textFromReq.isBlank()) {
            res = aiService.generateQuizFromText(textFromReq);
        } else {
            String pdf = ragService.getFullText(sessionId);
            if (pdf == null || pdf.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder().success(false).message("No source provided for quiz").build());
            }
            res = aiService.generateQuizFromText(pdf);
        }

        quizHistory.saveHistory(user, sessionId, res);
        return ResponseEntity.ok(ApiResponse.<String>builder().success(true).data(res).build());
    }

    @PostMapping("/translate")
    public ResponseEntity<ApiResponse<String>> translate(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = (principal != null) ? principal.getUser() : null;
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : request.getOrDefault("sessionId", "guest");
        String text = request.get("text");
        String target = (text != null && !text.isBlank()) ? text : ragService.getFullText(sessionId);
        if (target.isBlank()) return ResponseEntity.badRequest().body(ApiResponse.<String>builder().success(false).message("No content").build());
        
        String res = aiService.translateToBangla(target);
        translateHistory.saveHistory(user, sessionId, res);
        return ResponseEntity.ok(ApiResponse.<String>builder().success(true).data(res).build());
    }

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsage(
            @RequestParam String feature,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = (principal != null) ? principal.getUser() : null;
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : "guest"; 
        
        Map<String, Object> data = new HashMap<>();
        data.put("remaining", conversationService.getRemainingRequests(user, sessionId, FeatureEnum.fromValue(feature)));
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder().success(true).data(data).build());
    }
}