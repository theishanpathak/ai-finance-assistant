package com.example.ai_finance_assistant.service;

import com.example.ai_finance_assistant.dto.openai.OpenAIRequest;
import com.example.ai_finance_assistant.dto.openai.OpenAIStreamResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;


@Service
public class OpenAIClient {

    private final WebClient webclient;
    private final ObjectMapper objectMapper;

    public OpenAIClient(@Value("${openai.api.key}") String apikey, ObjectMapper objectMapper){
        this.webclient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer "+apikey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.objectMapper = objectMapper;
    }

    public Flux<String> createChatCompletion(OpenAIRequest request){
        return webclient.post()
                .uri("/chat/completions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.equals("[DONE]"))
                .map(this::extractContent);
    }

    private String extractContent(String jsonLine) {
        try {
            // Parse the raw JSON directly (no "data:" prefix)
            OpenAIStreamResponse response = objectMapper.readValue(jsonLine, OpenAIStreamResponse.class);
            if (response.choices() != null && !response.choices().isEmpty()) {
                OpenAIStreamResponse.Delta delta = response.choices().get(0).delta();
                if (delta != null && delta.content() != null) {
                    String content = delta.content();

                    return content;
                }
            }
        } catch (Exception e) {
            System.err.println("Parse error: " + e.getMessage());
        }
        return "";
    }
}
