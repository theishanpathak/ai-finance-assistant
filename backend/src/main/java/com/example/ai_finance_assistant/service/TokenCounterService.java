package com.example.ai_finance_assistant.service;

import org.springframework.stereotype.Service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;

@Service
public class TokenCounterService {

    private final Encoding enc;


    public TokenCounterService() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.enc = registry.getEncodingForModel(ModelType.GPT_4O_MINI);;
    }

    public int countTokens(String message){
//        IntArrayList encoded = enc.encode(message);  commented out so i know what I did for future reference
        return enc.encode(message).size();
    }
}
