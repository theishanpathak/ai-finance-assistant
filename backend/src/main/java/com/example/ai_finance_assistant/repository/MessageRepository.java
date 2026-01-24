package com.example.ai_finance_assistant.repository;

import com.example.ai_finance_assistant.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    //load all messages of a conversation by conversationId
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
