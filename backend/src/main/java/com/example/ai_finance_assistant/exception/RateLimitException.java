package com.example.ai_finance_assistant.exception;

public class RateLimitException extends OpenAIException{
    public RateLimitException(String message){
        super(message, 429);
    }
}
