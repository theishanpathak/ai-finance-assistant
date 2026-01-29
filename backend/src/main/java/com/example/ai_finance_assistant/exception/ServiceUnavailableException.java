package com.example.ai_finance_assistant.exception;

public class ServiceUnavailableException extends OpenAIException{
    public ServiceUnavailableException(String message){
        super(message, 503);
    }
}
