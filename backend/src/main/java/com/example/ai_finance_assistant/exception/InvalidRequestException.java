package com.example.ai_finance_assistant.exception;

public class InvalidRequestException extends OpenAIException{
    public InvalidRequestException(String message){
        super(message, 400);
    }
}
