package com.example.unogame.model.interfaces;

import com.example.unogame.model.UnoCard;

public interface IGameModel {
    UnoCard drawCard(boolean isUser);
    UnoCard playCard(UnoCard card, boolean isUser);
    boolean isValidPlay(UnoCard card);
    boolean isGameOver();
    void switchTurn();
}