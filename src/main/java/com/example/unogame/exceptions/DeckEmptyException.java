package com.example.unogame.exceptions;

public class DeckEmptyException extends Exception {
    public DeckEmptyException() {
        super("No hay más cartas disponibles en el mazo");
    }

    public DeckEmptyException(String message) {
        super(message);
    }
}