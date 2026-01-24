package com.example.ai_finance_assistant.repository;


import com.example.ai_finance_assistant.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    //find conversation by sessionID
    Optional<Conversation> findBySessionId(UUID sessionID);
}
