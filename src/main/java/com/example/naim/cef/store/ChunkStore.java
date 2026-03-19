package com.example.naim.cef.store;

import com.example.naim.cef.model.Chunk;
import java.util.List;

public interface ChunkStore {
    void save(Chunk chunk);
    void saveAll(List<Chunk> chunks);
    List<Chunk> similaritySearch(String docId, float[] queryEmbedding, int topK);
    List<Chunk> findByDocId(String docId);
    void deleteByDocId(String docId);
}
