package com.example.ai_finance_assistant.dto;

import java.time.LocalDateTime;

public record ConversationDTO(
        String role,
        String content,
        LocalDateTime timeStamp
) {
}
