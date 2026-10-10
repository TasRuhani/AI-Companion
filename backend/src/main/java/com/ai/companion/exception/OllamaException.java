package com.ai.companion.exception;

public class OllamaException extends RuntimeException {

    public OllamaException(String message, Throwable cause) {
        super(message, cause);
    }
}