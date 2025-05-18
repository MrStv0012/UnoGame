package com.example.unogame.exceptions;

/**
 * Thrown when attempting to draw from an empty deck.
 *
 * @authors Jhon Steven Angulo Nieves, Braulio Robledo Delgado
 */
public class DeckEmptyException extends Exception {
    public DeckEmptyException() {
        super("No hay más cartas disponibles en el mazo");
    }

    public DeckEmptyException(String message) {
        super(message);
    }
}