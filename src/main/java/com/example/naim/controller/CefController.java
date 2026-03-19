package com.example.naim.controller;

import com.example.naim.cef.agent.McpAgentLayer;
import com.example.naim.cef.model.Edge;
import com.example.naim.cef.model.Node;
import com.example.naim.cef.store.GraphStore;
import com.example.naim.dto.ApiResponse;
import com.example.naim.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cef")
@RequiredArgsConstructor
public class CefController {

    private final McpAgentLayer mcpAgentLayer;
    private final GraphStore    graphStore;

    @PostMapping("/agent/route")
    public ResponseEntity<ApiResponse<McpAgentLayer.AgentResponse>> handleAgentIntent(
            @RequestBody Map<String, String> request,
            HttpSession session,
            @AuthenticationPrincipal UserPrincipal principal) {
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : session.getId();
        try {
            String intent = request.get("intent");
            if (intent == null || intent.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.<McpAgentLayer.AgentResponse>builder().success(false).message("Intent cannot be empty.").build());
            }
            McpAgentLayer.AgentResponse response = mcpAgentLayer.routeAndExecute(intent, sessionId);
            return ResponseEntity.ok(ApiResponse.<McpAgentLayer.AgentResponse>builder().success(true).data(response).build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.<McpAgentLayer.AgentResponse>builder().success(false).message("Agent failure: " + e.getMessage()).build());
        }
    }

    @GetMapping("/graph/nodes")
    public ResponseEntity<ApiResponse<List<Node>>> getGraphNodes(HttpSession session, @AuthenticationPrincipal UserPrincipal principal) {
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : session.getId();
        return ResponseEntity.ok(ApiResponse.<List<Node>>builder()
            .success(true)
            .data(graphStore.findNodesByDocId(sessionId))
            .build());
    }

    @GetMapping("/graph/edges")
    public ResponseEntity<ApiResponse<List<Edge>>> getGraphEdges(HttpSession session, @AuthenticationPrincipal UserPrincipal principal) {
        String sessionId = (principal != null) ? principal.getUser().getId().toString() : session.getId();
        return ResponseEntity.ok(ApiResponse.<List<Edge>>builder()
            .success(true)
            .data(graphStore.findEdgesByDocId(sessionId))
            .build());
    }
}
