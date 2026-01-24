package com.example.ai_finance_assistant.dto;

public record ChatRequest(
        String message,
        String sessionID
) {}
