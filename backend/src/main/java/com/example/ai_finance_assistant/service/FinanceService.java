package com.example.ai_finance_assistant.service;

import com.example.ai_finance_assistant.dto.openai.OpenAIMessage;
import com.example.ai_finance_assistant.dto.openai.OpenAIRequest;
import com.example.ai_finance_assistant.entity.Conversation;
import com.example.ai_finance_assistant.entity.Message;
import com.example.ai_finance_assistant.repository.ConversationRepository;
import com.example.ai_finance_assistant.repository.MessageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FinanceService {
    private final OpenAIClient openAIClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    private final TokenCounterService tokenCounterService;

    private final int MAX_TOKENS = 2000;

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
        int total = 0;

        for(Message msg: messages){
            total += msg.getTokens();
        }
        return total;
    }


    private void summarizeHistory(List<Message> history) {
        System.out.println("Summary Triggered");
        List<OpenAIMessage> messages = new ArrayList<>();
        for(Message msg: history){
            messageRepository.delete(msg);
            messages.add(new OpenAIMessage(msg.getRole(), msg.getContent()));
        }
        messages.add(new OpenAIMessage("system",
                "You are summarizing the conversation so it can be used as memory for future turns. " +
                        "- Keep it concise, around 120 tokens, max 150.\n" +
                        "- Focus on key points:\n" +
                        "  - User goals and intent\n" +
                        "  - Decisions made\n" +
                        "  - Constraints or preferences\n" +
                        "  - Open questions or unresolved items\n" +
                        "- Use bullets or short numbered lists to organize information.\n" +
                        "- Maintain the tone and context of the conversation for continuity.\n" +
                        "- Do NOT include exact dialogue, repeated text, or unnecessary details.\n" +
                        "- Format as plain text suitable to be injected back as conversation history."
        ));

        OpenAIRequest request = new OpenAIRequest(
                "gpt-4o-mini",
                messages,
                150,
                true
        );

        StringBuilder assistantResponse = new StringBuilder();
        openAIClient.createChatCompletion(request)
                .doOnNext(chunk -> assistantResponse.append(chunk))//collect chunks
                .doOnComplete(() -> {
                    //save assistant response after streaming completes
                    saveMessage(history.get(0).getConversation().getId(), "assistant", assistantResponse.toString());

                });
    }


    public Flux<String> getResponseStream(String userMessage, String sessionId){

        //first find or create a new conversation
        Conversation conversation = findOrCreateConversation(sessionId);

        //load conversation history
        List<Message> history = loadConversationHistory(conversation.getId());

        //calculate total tokens, if more than MAX go ahead and reduce the history, but first count
        //and if still doesn't come within the limit, go ahead and truncate more
        int totalToken = calculateTotalTokens(history);
        if(totalToken > MAX_TOKENS){
            summarizeHistory(history);
        }


        //saving user message
        saveMessage(conversation.getId(), "user", userMessage);

        //build message array
        List<OpenAIMessage> messages = new ArrayList<>();

        messages.add(new OpenAIMessage("system",
                "You are a finance tutor. Only answer finance-related questions. " +
                        "Provide clear explanations using proper Markdown formatting. " +
                        "- Use numbered lists for step-by-step instructions.\n" +
                        "- Use bullets for examples or additional points.\n" +
                        "- Use **bold** for key terms or concepts.\n" +
                        "- Avoid complex math or formulas in LaTeX; use plain text instead.\n" +
                        "If asked non-finance questions, politely redirect to finance topics.\n" +
                        "You may answer questions about earlier parts of the conversation only if they are related to finance or financial goals."
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
                    saveMessage(conversation.getId(), "assistant", assistantResponse.toString());
                });
    }




}
