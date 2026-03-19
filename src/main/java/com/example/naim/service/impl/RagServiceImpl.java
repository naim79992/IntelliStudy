package com.example.naim.service.impl;

import com.example.naim.cef.indexer.KnowledgeIndexer;
import com.example.naim.cef.model.Chunk;
import com.example.naim.cef.retriever.KnowledgeRetriever;
import com.example.naim.service.interfaces.IRagService;
import com.example.naim.service.interfaces.IRedisCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements IRagService {
    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    private final Map<String, List<Chunk>> documentChunks = new ConcurrentHashMap<>();
    private final Map<String, String>      fullTexts       = new ConcurrentHashMap<>();
    private final Map<String, String>      documentNames   = new ConcurrentHashMap<>();
    
    private final IRedisCacheService       redisCache;
    private final KnowledgeIndexer         cefIndexer;
    private final KnowledgeRetriever       cefRetriever;
    private final DocumentParserServiceImpl parserService;
    private final TextChunkingStrategyImpl  chunkingStrategy;

    @Override
    public String extractAndStore(MultipartFile file, String sessionId) throws IOException {
        DocumentParserServiceImpl.ParsedDocument doc = parserService.parsePdf(file);
        String text = doc.text();
        String name = file.getOriginalFilename();

        fullTexts.put(sessionId, text);
        documentNames.put(sessionId, name);
        
        List<Chunk> chunks = chunkingStrategy.buildChunks(text, sessionId, 900, 150);
        documentChunks.put(sessionId, chunks);

        redisCache.cachePdfText(sessionId, text);
        redisCache.cachePdfName(sessionId, name);
        redisCache.setPdfLoaded(sessionId, true);

        // CEF Integration
        log.info("Triggering CEF Knowledge Indexer for docId={}", sessionId);
        new Thread(() -> cefIndexer.index(text, sessionId)).start();

        return String.format("PDF '%s' processed: %d pages, %d chunks. Indexing...", name, doc.pageCount(), chunks.size());
    }

    @Override
    public String getRelevantContext(String sessionId, String query, int topK) {
        String cefContext = cefRetriever.retrieve(sessionId, query, topK);
        if (cefContext != null && !cefContext.isBlank()) {
            return cefContext;
        }

        log.warn("CEF retriever returned empty, falling back to TF-IDF for docId={}", sessionId);

        ensureChunksLoaded(sessionId);
        List<Chunk> chunks = documentChunks.getOrDefault(sessionId, Collections.emptyList());
        if (chunks.isEmpty()) return "";
        
        String[] queryTerms = tokenize(query);
        if (queryTerms.length == 0) return "";
        
        return chunks.stream()
            .map(c -> new AbstractMap.SimpleEntry<Chunk, Double>(c, score(c.getText(), queryTerms)))
                .filter(e -> e.getValue() > 0)
                .sorted(Comparator.comparingDouble((AbstractMap.SimpleEntry<Chunk, Double> e) -> e.getValue()).reversed())
                .limit(topK)
                .map(e -> e.getKey().getText())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    @Override
    public String getRelevantContext(String sessionId, String query) {
        return getRelevantContext(sessionId, query, 3);
    }

    @Override
    public String getFullText(String sessionId) {
        String text = fullTexts.get(sessionId);
        if (text != null) return text;
        Optional<String> cached = redisCache.getCachedPdfText(sessionId);
        if (cached.isPresent()) { 
            fullTexts.put(sessionId, cached.get()); 
            return cached.get(); 
        }
        return "";
    }

    @Override
    public String getDocumentName(String sessionId) {
        String name = documentNames.get(sessionId);
        if (name != null) return name;
        return redisCache.getCachedPdfName(sessionId).orElse("Uploaded Document");
    }

    @Override
    public boolean hasDocument(String sessionId) {
        if (documentChunks.containsKey(sessionId) && !documentChunks.get(sessionId).isEmpty()) return true;
        return redisCache.isPdfLoaded(sessionId);
    }

    @Override
    public void clearSession(String sessionId) {
        documentChunks.remove(sessionId); 
        fullTexts.remove(sessionId); 
        documentNames.remove(sessionId);
        redisCache.clearPdfCache(sessionId);
    }

    private void ensureChunksLoaded(String sessionId) {
        if (documentChunks.containsKey(sessionId)) return;
        Optional<String> cachedText = redisCache.getCachedPdfText(sessionId);
        if (cachedText.isPresent()) {
            String text = cachedText.get();
            fullTexts.put(sessionId, text);
            documentNames.put(sessionId, redisCache.getCachedPdfName(sessionId).orElse("Cached Document"));
            documentChunks.put(sessionId, chunkingStrategy.buildChunks(text, sessionId, 900, 150));
        }
    }

    private double score(String chunkText, String[] queryTerms) {
        String lower = chunkText.toLowerCase(); 
        double score = 0;
        for (String term : queryTerms) {
            if (term.length() < 3) continue;
            int count = countOccurrences(lower, term);
            if (count > 0) score += count * (1.0 + (term.length() - 3) * 0.15);
        }
        return score;
    }

    private int countOccurrences(String text, String term) {
        int count = 0, i = 0;
        while ((i = text.indexOf(term, i)) != -1) { count++; i += term.length(); }
        return count;
    }

    private String[] tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("[\\s\\p{Punct}]+"))
            .filter(t -> !t.isBlank()).distinct().toArray(String[]::new);
    }
}