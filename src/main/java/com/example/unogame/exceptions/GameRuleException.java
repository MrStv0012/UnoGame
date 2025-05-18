package com.example.unogame.exceptions;

/**
 * Thrown when a game rule is violated.
 *
 * @authors Jhon Steven Angulo Nieves, Braulio Robledo Delgado
 */
public class GameRuleException extends RuntimeException {
    public GameRuleException(String message) {
        super(message);
    }

    public GameRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}

