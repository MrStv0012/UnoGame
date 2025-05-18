package com.example.unogame.exceptions;

/**
 * Thrown when an invalid card play is attempted.
 *
 * @authors Jhon Steven Angulo Nieves, Braulio Robledo Delgado
 */
public class InvalidCardPlayException extends Exception {
    public InvalidCardPlayException(String message) {
        super(message);
    }

    public InvalidCardPlayException(String message, Throwable cause) {
        super(message, cause);
    }
}
