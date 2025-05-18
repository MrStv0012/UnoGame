package com.example.unogame.exceptions;

public class GameRuleException extends RuntimeException {
    public GameRuleException(String message) {
        super(message);
    }

    public GameRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}

