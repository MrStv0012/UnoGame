package com.example.unogame.exceptions;

public class InvalidCardPlayException extends Exception {
    public InvalidCardPlayException(String message) {
        super(message);
    }

    public InvalidCardPlayException(String message, Throwable cause) {
        super(message, cause);
    }
}
