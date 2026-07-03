package dev.langchain4j.service;

public class TokenStreamLimitException extends RuntimeException {
    public TokenStreamLimitException(String message) {
        super(message);
    }
}
