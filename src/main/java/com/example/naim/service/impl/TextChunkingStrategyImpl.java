package com.example.naim.service.impl;

import com.example.naim.cef.model.Chunk;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Component
public class TextChunkingStrategyImpl {

    public List<Chunk> buildChunks(String text, String docId, int maxChunkChars, int overlapChars) {
        List<String> sentences = splitSentences(text);
        List<Chunk> chunks = new ArrayList<>();
        int idx = 0;
        StringBuilder current = new StringBuilder();
        Deque<String> overlap = new ArrayDeque<>();

        for (String sentence : sentences) {
            if (current.length() + sentence.length() > maxChunkChars && !current.isEmpty()) {
                chunks.add(new Chunk(idx++, docId, current.toString().strip()));
                
                // Build next chunk with overlap
                current = new StringBuilder();
                int currentOverlapLen = 0;
                for (String s : overlap) {
                    if (currentOverlapLen + s.length() < overlapChars) {
                        current.append(s).append(" ");
                        currentOverlapLen += s.length();
                    }
                }
                overlap.clear();
            }
            
            current.append(sentence).append(" ");
            overlap.addLast(sentence);
            
            // Maintain overlap sliding window
            while (calculateLength(overlap) > overlapChars && !overlap.isEmpty()) {
                overlap.pollFirst();
            }
        }

        if (!current.isEmpty()) {
            chunks.add(new Chunk(idx, docId, current.toString().strip()));
        }
        return chunks;
    }

    private int calculateLength(Deque<String> deque) {
        return deque.stream().mapToInt(String::length).sum();
    }

    private List<String> splitSentences(String text) {
        String[] raw = text.split("(?<=[.!?।])\\s+");
        List<String> sentences = new ArrayList<>();
        for (String r : raw) {
            r = r.strip();
            if (!r.isEmpty()) sentences.add(r);
        }
        return sentences;
    }
}
