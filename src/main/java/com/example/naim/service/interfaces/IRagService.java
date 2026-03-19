package com.example.naim.service.interfaces;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface IRagService {
    String extractAndStore(MultipartFile file, String sessionId) throws IOException;
    String getRelevantContext(String sessionId, String query, int topK);
    String getRelevantContext(String sessionId, String query);
    String getFullText(String sessionId);
    String getDocumentName(String sessionId);
    boolean hasDocument(String sessionId);
    void clearSession(String sessionId);
}
