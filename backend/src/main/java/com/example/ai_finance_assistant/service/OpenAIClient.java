package com.example.ai_finance_assistant.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.ai_finance_assistant.dto.openai.OpenAIRequest;
import com.example.ai_finance_assistant.dto.openai.OpenAIStreamResponse;
import com.example.ai_finance_assistant.exception.InvalidRequestException;
import com.example.ai_finance_assistant.exception.RateLimitException;
import com.example.ai_finance_assistant.exception.ServiceUnavailableException;

import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;
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
                .onStatus(
                        HttpStatus.TOO_MANY_REQUESTS::equals,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new RateLimitException("Rate Limit exceeded. Please try again later."))
                )
                .onStatus(
                        HttpStatus.BAD_REQUEST::equals,
                        response -> response.bodyToMono(String.class)
                                .map(body -> new InvalidRequestException("Invalid request: " + body))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new ServiceUnavailableException("OpenAI service unavailable: " + body))
                )
                .bodyToFlux(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof ServiceUnavailableException)
                        .doBeforeRetry(signal ->
                                System.out.println("Retrying request... Attempt: " + (signal.totalRetries() + 1))
                        )
                )
                .onErrorResume(WebClientResponseException.class, e -> {
                    System.out.println("Webclient error: " + e.getStatusCode() + " - " + e.getMessage());
                    return Flux.error(new ServiceUnavailableException("Failed to connect to OpenAI"));
                })
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
