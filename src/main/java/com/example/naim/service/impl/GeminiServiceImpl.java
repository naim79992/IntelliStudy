package com.example.naim.service.impl;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.example.naim.service.interfaces.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements AiService {
    private static final String MODEL = "gemini-2.5-flash";
    private final Client client;

    @Override
    public String generateResponse(String prompt) {
        GenerateContentResponse response = client.models.generateContent(MODEL, prompt, null);
        return response.text();
    }

    @Override
    public String summarizeText(String text) {
        String prompt = """
            Summarize within 100 tokens. You are an expert academic assistant. Summarize the following text clearly and concisely.
            Use bullet points for key points, then write a 2-3 sentence overall conclusion.
            Be accurate and do not add information not present in the text.
            TEXT:
            %s
            """.formatted(text);
        return generateResponse(prompt);
    }

    @Override
    public String translateToBangla(String text) {
        String prompt = """
            Translate the following text to fluent, natural Bangla (Bengali).
            Preserve the original meaning and tone. Do not add explanations — return only the translation.
            TEXT:
            %s
            """.formatted(text);
        return generateResponse(prompt);
    }

    @Override
    public String generateQuizJson(String topic) {
        return generateQuizJsonWithHistory("", topic);
    }

    @Override
    public String generateQuizJsonWithHistory(String historyBlock, String topic) {
        String context = historyBlock.isBlank() ? "" : """
            Note: The user has previously generated quizzes this session.
            %s
            Try to avoid repeating the exact same questions if a topic was covered before.
            """.formatted(historyBlock);
        String prompt = """
            You are a quiz generator. Create 5 challenging multiple-choice questions about: %s
            %s
            STRICT OUTPUT FORMAT — return ONLY a JSON array, no markdown, no extra text:
            [
              {
                "question": "Question text here",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "answer": "Exact text of the correct option"
              }
            ]
            Rules:
            - Each question must have exactly 4 options
            - The "answer" field must EXACTLY match one of the options
            - Questions should be educational and accurate
            - Vary difficulty (easy, medium, hard)
            """.formatted(topic, context);
        return extractJsonArray(generateResponse(prompt));
    }

    @Override
    public String generateQuizFromText(String text) {
        String prompt = """
            You are a quiz generator. Read the text below and create 5 multiple-choice questions
            that test understanding of the key concepts presented.
            STRICT OUTPUT FORMAT — return ONLY a JSON array, no markdown, no extra text:
            [
              {
                "question": "Question text here",
                "options": ["Option A", "Option B", "Option C", "Option D"],
                "answer": "Exact text of the correct option"
              }
            ]
            Rules:
            - Each question must have exactly 4 options
            - The "answer" field must EXACTLY match one of the options
            - Base questions ONLY on the provided text
            TEXT:
            %s
            """.formatted(text);
        return extractJsonArray(generateResponse(prompt));
    }

    @Override
    public String askWithContextAndHistory(String context, String historyBlock, String question) {
        String historySection = historyBlock.isBlank() ? "" : """
            CONVERSATION HISTORY (previous Q&A in this session):
            %s
            Use the history to understand follow-up questions like "explain more",
            "what about...", "and the next one?", etc.
            """.formatted(historyBlock);
        String prompt = """
            You are a precise document assistant with memory of the current conversation.
            Instructions:
            - Answer the question using ONLY the document context provided below.
            - If it's a follow-up question (e.g. "explain more", "give an example"),
              use the conversation history to understand what to elaborate on.
            - If the answer is NOT in the context, respond:
              "I could not find this information in the uploaded document."
            - Never fabricate information. Never use external knowledge.
            - Format your answer clearly. Use bullet points when listing multiple items.
            DOCUMENT CONTEXT:
            ---
            %s
            ---
            %s
            QUESTION: %s
            ANSWER:
            """.formatted(context, historySection, question);
        return generateResponse(prompt);
    }

    @Override
    public String summarizePdf(String pdfText) {
        String prompt = """
            You are a document analyst. Provide a comprehensive summary of the following document.
            Structure your summary as:
            1. **Overview** (2-3 sentences about what the document is about)
            2. **Key Points** (bullet list of the most important information)
            3. **Conclusion** (1-2 sentences on the overall message or findings)
            DOCUMENT TEXT:
            %s
            """.formatted(pdfText);
        return generateResponse(prompt);
    }

    private String extractJsonArray(String raw) {
        raw = raw.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").strip();
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            return raw.substring(start, end + 1);
        }
        return "[]";
    }
}