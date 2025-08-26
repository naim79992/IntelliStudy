package com.example.naim.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.example.naim.service.GeminiService;

@RestController
@RequestMapping("/api/gemini")
@RequiredArgsConstructor
public class GeminiController {

    private final GeminiService geminiService;

    @PostMapping("/ask")
    public String askGemini(@RequestBody String prompt) {
        return geminiService.askGemini(prompt);
    }

    // Summarize text
    @PostMapping("/summarize")
    public String summarize(@RequestBody String text) {
        return geminiService.summarizeText(text);
    }

     // Translate to Bangla
    @PostMapping("/translate")
    public String translate(@RequestBody String text) {
        return geminiService.translateToBangla(text);
    }

    // Generate quiz on specific topic
    // @PostMapping("/quiz/topic")
    // public String quizTopic(@RequestBody String topic) {
    //     return geminiService.generateQuiz(topic);
    // }
    // Generate quiz in JSON
@PostMapping("/quiz/topic-json")
public String quizTopicJson(@RequestBody String topic) {
    return geminiService.generateQuizJson(topic);
}

// Generate quiz from text
@PostMapping("/quiz/text-json")
public String quizFromText(@RequestBody String text) {
    return geminiService.generateQuizFromText(text);
}


}
//summarize//translate
//can learn more on specific topic
//can give quiz on specific text
//can give quiz on specific topic
