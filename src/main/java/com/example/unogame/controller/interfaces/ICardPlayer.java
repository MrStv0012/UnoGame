package com.example.unogame.controller.interfaces;

import com.example.unogame.exceptions.DeckEmptyException;
import com.example.unogame.exceptions.InvalidCardPlayException;
import com.example.unogame.model.UnoCard;

public interface ICardPlayer {
    void playTurn() throws InvalidCardPlayException, DeckEmptyException;
    UnoCard drawCard() throws DeckEmptyException;
    boolean canPlayCard();
    void declareUno();
}

