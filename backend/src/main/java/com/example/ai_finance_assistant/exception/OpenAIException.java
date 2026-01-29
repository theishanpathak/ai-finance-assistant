package com.example.ai_finance_assistant.exception;

public class OpenAIException extends RuntimeException{
    private final int statusCode;

    public OpenAIException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
