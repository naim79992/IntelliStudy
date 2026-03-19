package com.example.naim.controller;

import com.example.naim.dto.ApiResponse;
import com.example.naim.dto.MessageDto;
import com.example.naim.service.interfaces.IRagService;
import com.example.naim.service.interfaces.IRedisCacheService;
import com.example.naim.enums.FeatureEnum;
import com.example.naim.model.User;
import com.example.naim.service.interfaces.ConversationInterface;
import com.example.naim.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final IRagService ragService;
    private final IRedisCacheService redisCache;
    private final ConversationInterface conversationService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : "guest"; 
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder().success(false).message("File is empty").build());
            }
            if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder().success(false).message("Only PDF files are allowed").build());
            }

            String msg = ragService.extractAndStore(file, sessionId);
            return ResponseEntity.ok(ApiResponse.<String>builder().success(true).data(msg).build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder().success(false).message("Upload failed: " + e.getMessage()).build());
        }
    }

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<String>> askQuestion(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : request.getOrDefault("sessionId", "guest");
        try {
            String question = request.get("question");
            if (question == null || question.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder().success(false).message("Question cannot be empty").build());
            }

            if (!ragService.hasDocument(sessionId)) {
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder().success(false).message("Please upload a PDF first").build());
            }

            String context = ragService.getRelevantContext(sessionId, question);
            return ResponseEntity.ok(ApiResponse.<String>builder().success(true).data(context).build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder().success(false).message("Error: " + e.getMessage()).build());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(@AuthenticationPrincipal UserPrincipal principal) {
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : "guest"; 
        boolean loaded = ragService.hasDocument(sessionId);
        String name = loaded ? ragService.getDocumentName(sessionId) : null;
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
            .success(true)
            .data(Map.of("hasDocument", loaded, "documentName", name != null ? name : "None"))
            .build());
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clear(@AuthenticationPrincipal UserPrincipal principal) {
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : "guest";
        ragService.clearSession(sessionId);
        return ResponseEntity.ok(ApiResponse.<String>builder().success(true).data("Session cleared").build());
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getHistory(
            @RequestParam(defaultValue = "ask") String feature,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = (principal != null) ? principal.getUser() : null;
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : "guest"; 
        
        return ResponseEntity.ok(ApiResponse.<List<Map<String, String>>>builder()
            .success(true)
            .data(redisCache.getConversation(user != null ? user.getId().toString() : sessionId, feature))
            .build());
    }
}