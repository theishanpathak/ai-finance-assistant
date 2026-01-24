package com.example.ai_finance_assistant.dto;

import com.example.ai_finance_assistant.entity.Message;

import java.util.List;

public record ConversationResponse(String sessionID, List<Message> messages) {
}
