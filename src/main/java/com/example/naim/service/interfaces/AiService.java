package com.example.naim.service.interfaces;

public interface AiService {
    String generateResponse(String prompt);
    String summarizeText(String text);
    String translateToBangla(String text);
    String generateQuizJson(String topic);
    String generateQuizJsonWithHistory(String historyBlock, String topic);
    String generateQuizFromText(String text);
    String askWithContextAndHistory(String context, String historyBlock, String question);
    String summarizePdf(String pdfText);
    // Add more as needed for extensibility
}