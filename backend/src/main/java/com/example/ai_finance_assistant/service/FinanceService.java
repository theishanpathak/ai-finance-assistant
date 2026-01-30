package com.example.ai_finance_assistant.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.ai_finance_assistant.dto.openai.OpenAIMessage;
import com.example.ai_finance_assistant.dto.openai.OpenAIRequest;
import com.example.ai_finance_assistant.entity.Conversation;
import com.example.ai_finance_assistant.entity.Message;
import com.example.ai_finance_assistant.repository.ConversationRepository;
import com.example.ai_finance_assistant.repository.MessageRepository;

import reactor.core.publisher.Flux;

@Service
public class FinanceService {
    private final OpenAIClient openAIClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    private final TokenCounterService tokenCounterService;

    private final int MAX_TOKENS = 1500;

    public FinanceService(OpenAIClient openAIClient,
                          ConversationRepository conversationRepository,
                          MessageRepository messageRepository, TokenCounterService tokenCounterService) {
        this.openAIClient = openAIClient;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.tokenCounterService = tokenCounterService;
    }



    public Conversation findOrCreateConversation(String sessionIdStr){
        try{
            //convert the string to UUID
            UUID sessionID = UUID.fromString(sessionIdStr);

            //check to see if the conversation exist, if it does return or create a new one
            return conversationRepository.findBySessionId(sessionID)
                        .orElseGet(() -> {
                            Conversation newConv = new Conversation(sessionID);
                            return conversationRepository.save(newConv);
                        });

        //fails in case the sessionID is not valid
        }catch(IllegalArgumentException e){
            throw new RuntimeException("Invalid session ID format: " + sessionIdStr);
        }
    }


    //load all the messages associated with the conversation
    public List<Message> loadConversationHistory(Long conversationID){
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationID);
    }

    //helper function to save messages to the db
    public Message saveMessage(Long conversationID, String role, String content){
        Conversation conversation = conversationRepository.findById(conversationID)
                .orElseThrow(() -> new RuntimeException("Conversation not valid"));
        int tokenCount = tokenCounterService.countTokens(content);
        Message message = new Message(role, content, conversation, tokenCount);
        return messageRepository.save(message);
    }


    //loop through all the messages and give back the total number of tokens
    private int calculateTotalTokens(List<Message> messages){
        //declarative piece with stream
        return messages.stream()
                .mapToInt(Message::getTokens)
                .sum();

//        Classic way of doing it
    /*
      int total = 0;

        for(Message msg: messages){
            total += msg.getTokens();
       }
       return total;
    */
    }

    private List<Message> truncateHistory(List<Message> history, int totalTokens) {
        System.out.println("Truncating history: " + history.size() + " messages and total tokens " + totalTokens);
        List<Message> truncated = new ArrayList<>(history);

        while(totalTokens > MAX_TOKENS && truncated.size() > 1){
            Message oldest = truncated.remove(0);
            totalTokens -= oldest.getTokens();
            System.out.println("Dropped messages, tokens now " + totalTokens);
        }

        System.out.println("Kept " + truncated.size() + " of" + history.size() + " messages");

        return truncated;
    }



    public Flux<String> getResponseStream(String userMessage, String sessionId){

        //first find or create a new conversation
        Conversation conversation = findOrCreateConversation(sessionId);

        //load conversation history and truncate if necessary
        List<Message> history = loadConversationHistory(conversation.getId());
        int totalToken = calculateTotalTokens(history);

        if(totalToken > MAX_TOKENS){
            history = truncateHistory(history, totalToken); //truncate history
        }


        //saving user message
        saveMessage(conversation.getId(), "user", userMessage);

        //build message array
        List<OpenAIMessage> messages = new ArrayList<>();

        messages.add(new OpenAIMessage("system",
                        "You are an expert Finance Tutor. Only address finance-related topics. " +
                        "If asked non-finance questions, politely redirect to financial learning goals.\n\n" +

                        "FORMATTING & MATH:\n" +
                        "- Numbered lists for steps; bullets for examples.\n" +
                        "- **Bold** key terms. Use plain text for formulas (no LaTeX).\n\n" +

                        "OUTPUT CONSTRAINTS (CRITICAL):\n" +
                        "- LIMIT: Keep your total response under 300 words (roughly 400-500 tokens).\n" +
                        "- COMPLETION: Ensure every response ends with a conclusive summary or a final closing sentence. " +
                        "Never stop mid-thought or mid-explanation.\n" +
                        "- CONCISENESS: Prioritize high-impact information to ensure the full explanation fits within the limit."
        ));

        //add conversation history
        for(Message msg: history){
            messages.add(new OpenAIMessage(msg.getRole(), msg.getContent()));
        }

        //add new user message
        messages.add(new OpenAIMessage("user", userMessage));

        //create OpenAI request
        OpenAIRequest request = new OpenAIRequest(
                "gpt-4o-mini",
                messages,
                500,
                true
        );

        //stream response and collect it to save later
        StringBuilder assistantResponse = new StringBuilder();
        return openAIClient.createChatCompletion(request)
                .doOnNext(chunk -> assistantResponse.append(chunk))//collect chunks
                .doOnComplete(() -> {
                    //save assistant response after streaming completes
                    if (assistantResponse.length() > 0) {
                        saveMessage(conversation.getId(), "assistant", assistantResponse.toString());
                    }
                });
    }
}
