package com.example.ai_finance_assistant.controller;

import com.example.ai_finance_assistant.dto.ChatRequest;
import com.example.ai_finance_assistant.service.FinanceService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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

}
