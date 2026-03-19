package com.example.naim.Controller;

import com.example.naim.dto.ApiResponse;
import com.example.naim.dto.MessageDto;
import com.example.naim.enums.FeatureEnum;
import com.example.naim.service.RagService;
import com.example.naim.service.interfaces.AiService;
import com.example.naim.service.interfaces.ConversationInterface;
import jakarta.servlet.http.HttpSession;
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
    private final RagService ragService;
    private final AiService aiService;
    private final ConversationInterface conversationService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadPdf(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        try {
            if (file.isEmpty())
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("No file selected. Please choose a PDF.")
                    .build());
            String name = file.getOriginalFilename();
            if (name == null || !name.toLowerCase().endsWith(".pdf"))
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Only PDF files are supported.")
                    .build());
            if (file.getSize() > 20 * 1024 * 1024)
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("File too large. Maximum size is 20 MB.")
                    .build());
            String result = ragService.extractAndStore(file, session.getId());
            conversationService.saveMessage(
                session.getId(), FeatureEnum.RAG,
                "assistant",
                "[System] PDF loaded: " + name + ". Ready to answer questions."
            );
            return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .data(result)
                .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                .success(false)
                .message("Failed to process PDF: " + e.getMessage())
                .build());
        }
    }

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<String>> askQuestion(
            @RequestBody Map<String, String> request,
            HttpSession session) {
        try {
            String question = request.get("question");
            if (question == null || question.isBlank())
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Question cannot be empty.")
                    .build());
            if (!ragService.hasDocument(session.getId()))
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Please upload a PDF first.")
                    .build());
            String sid = session.getId();
            if (!conversationService.isAllowed(sid, FeatureEnum.RAG))
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Too many requests. Please wait a moment.")
                    .build());
            conversationService.saveMessage(sid, FeatureEnum.RAG, "user", question);
            String context = ragService.getRelevantContext(sid, question, 4);
            if (context.isBlank()) {
                String fullText = ragService.getFullText(sid);
                context = fullText.substring(0, Math.min(3000, fullText.length()));
            }
            String history = conversationService.buildHistoryBlock(sid, FeatureEnum.RAG);
            String answer = aiService.askWithContextAndHistory(context, history, question);
            conversationService.saveMessage(sid, FeatureEnum.RAG, "assistant", answer);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .data(answer)
                .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                .success(false)
                .message("Failed to answer question: " + e.getMessage())
                .build());
        }
    }

    @PostMapping("/summarize")
    public ResponseEntity<ApiResponse<String>> summarizePdf(HttpSession session) {
        try {
            if (!ragService.hasDocument(session.getId()))
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Please upload a PDF first.")
                    .build());
            String fullText = ragService.getFullText(session.getId());
            String toSummarize = fullText.substring(0, Math.min(12000, fullText.length()));
            String summary = aiService.summarizePdf(toSummarize);
            conversationService.saveMessage(session.getId(), FeatureEnum.RAG, "user", "[Summarize request]");
            conversationService.saveMessage(session.getId(), FeatureEnum.RAG, "assistant", summary);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .data(summary)
                .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                .success(false)
                .message("Failed to summarize PDF: " + e.getMessage())
                .build());
        }
    }

    @PostMapping("/quiz")
    public ResponseEntity<ApiResponse<String>> generateQuizFromPdf(HttpSession session) {
        try {
            if (!ragService.hasDocument(session.getId()))
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message("Please upload a PDF first.")
                    .build());
            String fullText = ragService.getFullText(session.getId());
            String textForQuiz = fullText.substring(0, Math.min(8000, fullText.length()));
            String quiz = aiService.generateQuizFromText(textForQuiz);
            conversationService.saveMessage(session.getId(), FeatureEnum.QUIZ, "user",
                "Generate quiz from PDF: " + ragService.getDocumentName(session.getId()));
            conversationService.saveMessage(session.getId(), FeatureEnum.QUIZ, "assistant",
                "Generated quiz from PDF document.");
            return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .data(quiz)
                .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                .success(false)
                .message("Failed to generate quiz: " + e.getMessage())
                .build());
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearDocument(HttpSession session) {
        ragService.clearSession(session.getId());
        conversationService.clearFeature(session.getId(), FeatureEnum.RAG);
        return ResponseEntity.ok(ApiResponse.<String>builder()
            .success(true)
            .message("PDF and conversation history removed successfully.")
            .build());
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkStatus(HttpSession session) {
        boolean has = ragService.hasDocument(session.getId());
        String name = has ? ragService.getDocumentName(session.getId()) : null;
        boolean hasHist = conversationService.hasHistory(session.getId(), FeatureEnum.RAG);
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
            .success(true)
            .data(Map.of(
                "hasDocument", has,
                "documentName", name != null ? name : "",
                "hasHistory", hasHist
            ))
            .build());
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getHistory(HttpSession session) {
        List<MessageDto> messages = conversationService.loadMessages(
            session.getId(), FeatureEnum.RAG, 50);
        return ResponseEntity.ok(ApiResponse.<List<MessageDto>>builder()
            .success(true)
            .data(messages)
            .build());
    }
}