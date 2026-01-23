package com.example.ai_finance_assistant.dto.openai;

import java.util.List;

public record OpenAIStreamResponse(List<Choice> choices) {
    public record Choice(Delta delta){}
    public record Delta(String content){}
}
