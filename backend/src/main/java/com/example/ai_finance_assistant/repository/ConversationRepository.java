package com.example.ai_finance_assistant.repository;


import com.example.ai_finance_assistant.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {}
