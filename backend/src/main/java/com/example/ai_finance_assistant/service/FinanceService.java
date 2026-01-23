package com.example.ai_finance_assistant.service;

import com.example.ai_finance_assistant.dto.openai.OpenAIMessage;
import com.example.ai_finance_assistant.dto.openai.OpenAIRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class FinanceService {
    private final OpenAIClient openAIClient;

    public FinanceService(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    public Flux<String> getResponseStream(String message){
        OpenAIMessage systemMsg = new OpenAIMessage("system",
                "You are a finance tutor. Only answer finance-related questions. " +
                        "Provide clear explanations using proper Markdown formatting. " +
                        "- Use numbered lists for step-by-step instructions.\n" +
                        "- Use bullets for examples or additional points.\n" +
                        "- Use **bold** for key terms or concepts.\n" +
                        "- Avoid complex math or formulas in LaTeX; use plain text instead.\n" +
                        "If asked non-finance questions, politely redirect to finance topics."
        );
        OpenAIMessage userMsg = new OpenAIMessage("user", message);
        OpenAIRequest request = new OpenAIRequest(
                "gpt-4o-mini",
                List.of(systemMsg, userMsg),
                500,
                true
        );
        return openAIClient.createChatCompletion(request);
    }
}
