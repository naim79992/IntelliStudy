package com.example.naim.cef.agent;

import com.example.naim.service.interfaces.IRagService;
import com.example.naim.service.interfaces.AiService;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class McpAgentLayer {
    private static final Logger log = LoggerFactory.getLogger(McpAgentLayer.class);

    private final Client     geminiClient;
    private final AiService  aiService;
    private final IRagService ragService;

    private static final List<ToolDefinition> TOOLS = List.of(
        new ToolDefinition("RAG_ASK", "Answer questions based on the uploaded document context", "ragService.getRelevantContext"),
        new ToolDefinition("SUMMARIZE", "Summarize the entire uploaded document", "aiService.summarizePdf"),
        new ToolDefinition("QUIZ", "Generate a multiple-choice quiz from the document", "aiService.generateQuizFromText"),
        new ToolDefinition("TRANSLATE", "Translate text to Bangla", "aiService.translateToBangla")
    );

    public McpAgentLayer(Client geminiClient, AiService aiService, IRagService ragService) {
        this.geminiClient = geminiClient;
        this.aiService = aiService;
        this.ragService = ragService;
    }

    public AgentResponse routeAndExecute(String intent, String sessionId) {
        String toolSelectPrompt = buildToolSelectPrompt(intent);
        String selectedTool = "RAG_ASK";
        try {
            GenerateContentResponse response = geminiClient.models.generateContent("gemini-2.5-flash", toolSelectPrompt, null);
            selectedTool = response.text().strip().toUpperCase();
        } catch (Exception e) {
            log.warn("Gemini intent classification failed. Using default. Error: {}", e.getMessage());
        }

        boolean hasDoc = ragService.hasDocument(sessionId);
        if (!hasDoc && (selectedTool.equals("SUMMARIZE") || selectedTool.equals("QUIZ") || selectedTool.equals("RAG_ASK"))) {
            return new AgentResponse(selectedTool, "I need a document uploaded first before I can help with that.");
        }

        String result;
        try {
            result = switch (selectedTool) {
                case "SUMMARIZE" -> aiService.summarizePdf(getTruncatedText(sessionId, 12000));
                case "QUIZ" -> aiService.generateQuizFromText(getTruncatedText(sessionId, 8000));
                case "TRANSLATE" -> aiService.translateToBangla(getTruncatedText(sessionId, 2000));
                case "RAG_ASK" -> {
                    String context = ragService.getRelevantContext(sessionId, intent, 4);
                    if (context.isBlank()) context = getTruncatedText(sessionId, 3000);
                    yield aiService.askWithContextAndHistory(context, "", intent);
                }
                default -> "I'm not sure which tool to use for that request.";
            };
        } catch (Exception e) {
            result = "An error occurred while executing the tool: " + e.getMessage();
        }
        return new AgentResponse(selectedTool, result);
    }

    private String getTruncatedText(String sessionId, int maxChars) {
        String full = ragService.getFullText(sessionId);
        return full.substring(0, Math.min(maxChars, full.length()));
    }

    private String buildToolSelectPrompt(String userIntent) {
        StringBuilder sb = new StringBuilder("You are an MCP router. Analyze the user intent and select the EXCLUSIVE BEST TOOL from the following list:\n");
        for (ToolDefinition t : TOOLS) sb.append("- ").append(t.toolName()).append(" : ").append(t.description()).append("\n");
        sb.append("\nUser Intent: \"").append(userIntent).append("\"\n\nReturn ONLY the exact ToolName string (e.g. RAG_ASK, SUMMARIZE) and absolutely nothing else. No explanations.");
        return sb.toString();
    }

    public record AgentResponse(String toolUsed, String resultData) {}
}
