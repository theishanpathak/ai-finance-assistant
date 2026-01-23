package com.example.ai_finance_assistant.dto.openai;

import java.util.List;

public record OpenAIRequest(String model,
                            List<OpenAIMessage> messages,
                            Integer max_tokens,
                            Boolean stream) {
}
