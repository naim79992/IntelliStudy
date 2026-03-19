package com.example.naim.cef.store.duckdb;

import com.example.naim.cef.model.Chunk;
import com.example.naim.cef.store.ChunkStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.*;
import java.sql.*;
import java.util.*;

@Component
public class DuckDbChunkStore implements ChunkStore {
    private static final Logger log = LoggerFactory.getLogger(DuckDbChunkStore.class);
    private final Connection conn;

    public DuckDbChunkStore(@Qualifier("duckDbConnection") Connection conn) {
        this.conn = conn;
    }

    @Override
    public void save(Chunk chunk) {
        String sql = "INSERT OR REPLACE INTO cef_chunks (id, doc_id, text, embedding) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, chunk.getIndex());
            ps.setString(2, chunk.getDocId());
            ps.setString(3, chunk.getText());
            ps.setBytes(4, serializeEmbedding(chunk.getEmbedding()));
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to save chunk", e);
        }
    }

    @Override
    public void saveAll(List<Chunk> chunks) {
        for (Chunk c : chunks) save(c);
    }

    @Override
    public List<Chunk> similaritySearch(String docId, float[] queryEmbedding, int topK) {
        List<Chunk> all = findByDocId(docId);
        if (all.isEmpty() || queryEmbedding == null || queryEmbedding.length == 0) return Collections.emptyList();

        return all.stream()
            .filter(c -> c.getEmbedding() != null)
            .map(c -> Map.entry(c, cosineSimilarity(queryEmbedding, c.getEmbedding())))
            .filter(e -> e.getValue() > 0)
            .sorted(Map.Entry.<Chunk, Double>comparingByValue().reversed())
            .limit(topK)
            .map(Map.Entry::getKey)
            .toList();
    }

    @Override
    public List<Chunk> findByDocId(String docId) {
        List<Chunk> result = new ArrayList<>();
        String sql = "SELECT id, doc_id, text, embedding FROM cef_chunks WHERE doc_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, docId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Chunk c = new Chunk(rs.getInt("id"), rs.getString("doc_id"), rs.getString("text"));
                byte[] embBytes = rs.getBytes("embedding");
                if (embBytes != null) c.setEmbedding(deserializeEmbedding(embBytes));
                result.add(c);
            }
        } catch (SQLException e) {
            log.error("Failed to load chunks", e);
        }
        return result;
    }

    @Override
    public void deleteByDocId(String docId) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM cef_chunks WHERE doc_id = ?")) {
            ps.setString(1, docId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete chunks", e);
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0.0 : dot / denom;
    }

    private byte[] serializeEmbedding(float[] emb) {
        if (emb == null) return null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {
            for (float v : emb) dos.writeFloat(v);
            return bos.toByteArray();
        } catch (IOException e) { return null; }
    }

    private float[] deserializeEmbedding(byte[] bytes) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes))) {
            float[] emb = new float[bytes.length / 4];
            for (int i = 0; i < emb.length; i++) emb[i] = dis.readFloat();
            return emb;
        } catch (IOException e) { return null; }
    }
}
