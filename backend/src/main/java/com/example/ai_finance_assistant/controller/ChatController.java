package com.example.ai_finance_assistant.controller;

import com.example.ai_finance_assistant.dto.ChatRequest;
import com.example.ai_finance_assistant.dto.ConversationDTO;
import com.example.ai_finance_assistant.entity.Conversation;
import com.example.ai_finance_assistant.entity.Message;
import com.example.ai_finance_assistant.service.FinanceService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {
    private final FinanceService financeService;

    public ChatController(FinanceService financeService) {
        this.financeService = financeService;
    }


    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request){
        return financeService.getResponseStream(request.message(), request.sessionID());
    }

    @GetMapping("/history/{sessionId}")
    public List<ConversationDTO> getConversation(@PathVariable String sessionId){
        Conversation conversation = financeService.findOrCreateConversation(sessionId);
        List<Message> messages = financeService.loadConversationHistory(conversation.getId());

        return messages.stream()
                .map(msg -> new ConversationDTO(msg.getRole(), msg.getContent(), msg.getCreatedAt()))
                .toList();
    }

}
