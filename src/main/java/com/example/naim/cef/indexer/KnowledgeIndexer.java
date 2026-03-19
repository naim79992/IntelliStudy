package com.example.naim.cef.indexer;

import com.example.naim.cef.model.Chunk;
import com.example.naim.cef.model.Edge;
import com.example.naim.cef.model.Node;
import com.example.naim.cef.store.ChunkStore;
import com.example.naim.cef.store.GraphStore;
import com.example.naim.cef.store.duckdb.InMemoryGraph;
import com.example.naim.service.impl.TextChunkingStrategyImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeIndexer {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexer.class);
    private static final String EMBED_MODEL = "text-embedding-004";
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final ChunkStore               chunkStore;
    private final GraphStore               graphStore;
    private final InMemoryGraph             inMemoryGraph;
    private final HttpClient               httpClient;
    private final ObjectMapper             objectMapper;
    private final TextChunkingStrategyImpl chunkingStrategy;

    public KnowledgeIndexer(ChunkStore chunkStore, GraphStore graphStore, 
                            InMemoryGraph inMemoryGraph, TextChunkingStrategyImpl chunkingStrategy) {
        this.chunkStore       = chunkStore;
        this.graphStore       = graphStore;
        this.inMemoryGraph    = inMemoryGraph;
        this.chunkingStrategy = chunkingStrategy;
        this.httpClient       = HttpClient.newHttpClient();
        this.objectMapper     = new ObjectMapper();
    }

    public void index(String text, String docId) {
        chunkStore.deleteByDocId(docId);
        graphStore.deleteByDocId(docId);

        List<Chunk> chunks = chunkingStrategy.buildChunks(text, docId, 1000, 200);
        log.info("KnowledgeIndexer: built {} chunks for docId={}", chunks.size(), docId);

        for (Chunk chunk : chunks) {
            float[] embedding = embedText(chunk.getText());
            chunk.setEmbedding(embedding);
            chunkStore.save(chunk);
            extractGraphWithLLM(chunk.getText(), docId);
        }

        inMemoryGraph.refresh(docId);
        log.info("KnowledgeIndexer: Indexing complete for docId={}", docId);
    }

    public float[] embedText(String text) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + EMBED_MODEL + ":embedContent?key=" + geminiApiKey;
            Map<String, Object> part = Map.of("text", text);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> reqBody = Map.of("model", "models/" + EMBED_MODEL, "content", content);
            
            String jsonBody = objectMapper.writeValueAsString(reqBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode vals = root.path("embedding").path("values");
                if (vals.isArray()) {
                    float[] arr = new float[vals.size()];
                    for (int i = 0; i < vals.size(); i++) arr[i] = (float) vals.get(i).asDouble();
                    return arr;
                }
            } else { log.error("Embedding API error {}: {}", response.statusCode(), response.body()); }
        } catch (Exception e) { log.error("Failed to generate embedding: {}", e.getMessage()); }
        return new float[0];
    }

    private void extractGraphWithLLM(String text, String docId) {
        try {
            String prompt = """
                Extract a knowledge graph from the text below. 
                Identify main Entities/Concepts as Nodes and their Relationships as Edges.
                STRICT OUTPUT FORMAT — return ONLY a JSON object, no markdown, no extra text:
                {
                  "nodes": [{"label": "Concept Name", "type": "ENTITY|CONCEPT"}],
                  "edges": [{"from": "LabelA", "to": "LabelB", "relation": "relationship type"}]
                }
                TEXT:
                %s
                """.formatted(text);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
            Map<String, Object> reqBody = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
            
            String jsonBody = objectMapper.writeValueAsString(reqBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String rawJson = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                parseAndSaveGraph(rawJson, docId);
            }
        } catch (Exception e) { log.error("Failed to extract graph with LLM: {}", e.getMessage()); }
    }

    private void parseAndSaveGraph(String rawJson, String docId) {
        try {
            rawJson = rawJson.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").strip();
            JsonNode root = objectMapper.readTree(rawJson);
            
            JsonNode nodes = root.path("nodes");
            if (nodes.isArray()) {
                for (JsonNode n : nodes) {
                    String label = n.path("label").asText();
                    String type  = n.path("type").asText("CONCEPT");
                    if (!label.isBlank()) {
                        graphStore.saveNode(new Node(docId + "_" + label.toLowerCase(), docId, label, type, 1));
                    }
                }
            }
            
            JsonNode edges = root.path("edges");
            if (edges.isArray()) {
                for (JsonNode e : edges) {
                    String from = e.path("from").asText().toLowerCase();
                    String to   = e.path("to").asText().toLowerCase();
                    String rel  = e.path("relation").asText("RELATED_TO");
                    if (!from.isBlank() && !to.isBlank()) {
                        graphStore.saveEdge(new Edge(docId + "_" + from, docId + "_" + to, docId, rel, 1));
                    }
                }
            }
        } catch (Exception e) { log.warn("Failed to parse graph JSON: {}", e.getMessage()); }
    }
}
