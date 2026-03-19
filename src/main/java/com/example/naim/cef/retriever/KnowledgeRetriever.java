package com.example.naim.cef.retriever;

import com.example.naim.cef.indexer.KnowledgeIndexer;
import com.example.naim.cef.model.Chunk;
import com.example.naim.cef.store.ChunkStore;
import com.example.naim.cef.store.duckdb.InMemoryGraph;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeRetriever {
    private final ChunkStore       chunkStore;
    private final InMemoryGraph    inMemoryGraph;
    private final KnowledgeIndexer indexer;

    public KnowledgeRetriever(ChunkStore chunkStore, InMemoryGraph inMemoryGraph, KnowledgeIndexer indexer) {
        this.chunkStore    = chunkStore;
        this.inMemoryGraph = inMemoryGraph;
        this.indexer       = indexer;
    }

    public String retrieve(String docId, String query, int topK) {
        float[] queryEmbedding = indexer.embedText(query);
        if (queryEmbedding.length == 0) return "";

        List<Chunk> vectorTopChunks = chunkStore.similaritySearch(docId, queryEmbedding, topK * 2);
        if (vectorTopChunks.isEmpty()) return "";

        // Extract seed concepts from query (simple tokenization for now)
        Set<String> queryConcepts = Arrays.stream(query.split("\\s+"))
                .filter(w -> w.length() > 3).map(String::toLowerCase).collect(Collectors.toSet());

        // Find related concepts in the graph (depth 1 or 2)
        Set<String> graphContext = inMemoryGraph.findRelated(docId, queryConcepts, 2);

        Map<Chunk, Double> finalScores = new HashMap<>();
        for (int i = 0; i < vectorTopChunks.size(); i++) {
            Chunk c = vectorTopChunks.get(i);
            double vectorScore = 1.0 - (i * (1.0 / vectorTopChunks.size())); // Normalized vector rank
            
            double graphBoost = 0.0;
            String textLower = c.getText().toLowerCase();
            for (String gc : graphContext) {
                if (textLower.contains(gc)) graphBoost += 0.15; // Significant boost for graph-linked info
            }
            finalScores.put(c, vectorScore + graphBoost);
        }

        return finalScores.entrySet().stream()
            .sorted(Map.Entry.<Chunk, Double>comparingByValue().reversed())
            .limit(topK).map(e -> e.getKey().getText())
            .collect(Collectors.joining("\n\n---\n\n"));
    }
}
