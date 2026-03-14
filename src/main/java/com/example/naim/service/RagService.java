package com.example.naim.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final Map<String, List<Chunk>> documentChunks = new ConcurrentHashMap<>();
    private final Map<String, String>      fullTexts       = new ConcurrentHashMap<>();
    private final Map<String, String>      documentNames   = new ConcurrentHashMap<>();
    private final RedisCacheService        redisCache;

    public RagService(RedisCacheService redisCache) {
        this.redisCache = redisCache;
    }

    public String extractAndStore(MultipartFile file, String sessionId) throws IOException {
        PDDocument doc = Loader.loadPDF(file.getBytes());
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(doc);
        int pageCount = doc.getNumberOfPages();
        doc.close();

        text = text.replaceAll("\\r\\n|\\r", "\n").replaceAll("[ \\t]+", " ").strip();
        String name = file.getOriginalFilename();

        fullTexts.put(sessionId, text);
        documentNames.put(sessionId, name);
        List<Chunk> chunks = buildChunks(text, 900, 150);
        documentChunks.put(sessionId, chunks);

        // Cache in Redis so rebuild is possible after restart
        redisCache.cachePdfText(sessionId, text);
        redisCache.cachePdfName(sessionId, name);
        redisCache.setPdfLoaded(sessionId, true);

        return String.format("PDF '%s' processed: %d pages, %d chunks.", name, pageCount, chunks.size());
    }

    public String getRelevantContext(String sessionId, String query, int topK) {
        ensureChunksLoaded(sessionId);
        List<Chunk> chunks = documentChunks.getOrDefault(sessionId, Collections.emptyList());
        if (chunks.isEmpty()) return "";
        String[] queryTerms = tokenize(query);
        if (queryTerms.length == 0) return "";
        return chunks.stream()
            .map(c -> new AbstractMap.SimpleEntry<>(c, score(c.text(), queryTerms)))
            .filter(e -> e.getValue() > 0)
            .sorted(Comparator.comparingDouble(Map.Entry<Chunk, Double>::getValue).reversed())
            .limit(topK)
            .map(e -> e.getKey().text())
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    public String getRelevantContext(String sessionId, String query) {
        return getRelevantContext(sessionId, query, 3);
    }

    public String getFullText(String sessionId) {
        String text = fullTexts.get(sessionId);
        if (text != null) return text;
        Optional<String> cached = redisCache.getCachedPdfText(sessionId);
        if (cached.isPresent()) { fullTexts.put(sessionId, cached.get()); return cached.get(); }
        return "";
    }

    public String getDocumentName(String sessionId) {
        String name = documentNames.get(sessionId);
        if (name != null) return name;
        return redisCache.getCachedPdfName(sessionId).orElse("Uploaded Document");
    }

    public boolean hasDocument(String sessionId) {
        if (documentChunks.containsKey(sessionId) && !documentChunks.get(sessionId).isEmpty()) return true;
        return redisCache.isPdfLoaded(sessionId);
    }

    public void clearSession(String sessionId) {
        documentChunks.remove(sessionId); fullTexts.remove(sessionId); documentNames.remove(sessionId);
        redisCache.clearPdfCache(sessionId);
    }

    private void ensureChunksLoaded(String sessionId) {
        if (documentChunks.containsKey(sessionId)) return;
        Optional<String> cachedText = redisCache.getCachedPdfText(sessionId);
        if (cachedText.isPresent()) {
            String text = cachedText.get();
            fullTexts.put(sessionId, text);
            documentNames.put(sessionId, redisCache.getCachedPdfName(sessionId).orElse("Cached Document"));
            documentChunks.put(sessionId, buildChunks(text, 900, 150));
        }
    }

    private List<Chunk> buildChunks(String text, int maxChunkChars, int overlapChars) {
        List<String> sentences = splitSentences(text);
        List<Chunk> chunks = new ArrayList<>();
        int idx = 0;
        StringBuilder current = new StringBuilder();
        Deque<String> overlap = new ArrayDeque<>();
        for (String sentence : sentences) {
            if (current.length() + sentence.length() > maxChunkChars && !current.isEmpty()) {
                chunks.add(new Chunk(idx++, current.toString().strip()));
                current = new StringBuilder();
                int ob = 0;
                for (String s : overlap) { if (ob + s.length() < overlapChars) { current.append(s).append(" "); ob += s.length(); } }
                overlap.clear();
            }
            current.append(sentence).append(" ");
            overlap.addLast(sentence);
            while (overlap.stream().mapToInt(String::length).sum() > overlapChars && !overlap.isEmpty()) overlap.pollFirst();
        }
        if (!current.isEmpty()) chunks.add(new Chunk(idx, current.toString().strip()));
        return chunks;
    }

    private List<String> splitSentences(String text) {
        String[] raw = text.split("(?<=[.!?।])\\s+");
        List<String> s = new ArrayList<>();
        for (String r : raw) { r = r.strip(); if (!r.isEmpty()) s.add(r); }
        return s;
    }

    private double score(String chunkText, String[] queryTerms) {
        String lower = chunkText.toLowerCase(); double score = 0;
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

    private record Chunk(int index, String text) {}
}