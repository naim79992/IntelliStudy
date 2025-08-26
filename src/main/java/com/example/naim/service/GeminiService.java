package com.example.naim.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final Client client;

    public String askGemini(String prompt) {
        GenerateContentResponse response =
            client.models.generateContent(
                "gemini-2.5-flash",  // Model name
                prompt,
                null                // options (e.g., safety settings)
            );
        return response.text();
    }

     // Summarization endpoint
    public String summarizeText(String text) {
        String prompt = "Summarize the following text in a concise way:\n" + text;
        GenerateContentResponse response =
            client.models.generateContent(
                "gemini-2.5-flash",
                prompt,
                null
            );
        return response.text();
    }

     // Translation to Bangla
    public String translateToBangla(String text) {
        String prompt = "Translate the following text to Bangla:\n" + text;
        GenerateContentResponse response =
            client.models.generateContent(
                "gemini-2.5-flash",
                prompt,
                null
            );
        return response.text();
    }

    // Generate quiz on specific topic
    // public String generateQuiz(String topic) {
    //     String prompt = "Generate 5 multiple-choice questions with answers on the topic: " + topic;
    //     GenerateContentResponse response =
    //         client.models.generateContent(
    //             "gemini-2.5-flash",
    //             prompt,
    //             null
    //         );
    //     return response.text();
    // }
    // Generate quiz on specific topic as JSON
public String generateQuizJson(String topic) {
    // Strong prompt forcing strict JSON output
    String prompt = """
        Generate 5 multiple-choice questions on the topic: %s.
        Each question must be in EXACT JSON format like this:
        {
          "question": "Your question here",
          "options": ["Option1", "Option2", "Option3", "Option4"],
          "answer": "CorrectOption"
        }
        Return ONLY the JSON array, nothing else, no explanations, no extra text.
    """.formatted(topic);

    GenerateContentResponse response =
        client.models.generateContent(
            "gemini-2.5-flash",
            prompt,
            null
        );

    // Get the text
    String raw = response.text();

    // Optional: try to ensure only valid JSON is returned
    int start = raw.indexOf("[");
    int end = raw.lastIndexOf("]");
    if (start != -1 && end != -1) {
        return raw.substring(start, end + 1);
    } else {
        // fallback, return empty array if parsing fails
        return "[]";
    }
}

// Generate quiz based on full text
public String generateQuizFromText(String text) {
    String prompt = """
        Read the following text carefully and generate 5 multiple-choice questions based on it.
        Each question must be in EXACT JSON format like this:
        {
          "question": "Question from text",
          "options": ["Option1", "Option2", "Option3", "Option4"],
          "answer": "CorrectOption"
        }
        Return ONLY the JSON array, nothing else.
        Text:
        %s
    """.formatted(text);

    GenerateContentResponse response =
        client.models.generateContent(
            "gemini-2.5-flash",
            prompt,
            null
        );

    // Ensure only JSON array is returned
    String raw = response.text();
    int start = raw.indexOf("[");
    int end = raw.lastIndexOf("]");
    if (start != -1 && end != -1) {
        return raw.substring(start, end + 1);
    } else {
        return "[]";
    }
}

}
