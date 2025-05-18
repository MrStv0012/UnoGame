package com.example.unogame.adapters;

import com.example.unogame.controller.interfaces.ICardPlayer;
import com.example.unogame.exceptions.DeckEmptyException;
import com.example.unogame.exceptions.InvalidCardPlayException;
import com.example.unogame.model.UnoCard;
import com.example.unogame.model.GameModel;

/**
 * Adapter implementing ICardPlayer for user or CPU.
 *
 * @authors Jhon Steven Angulo Nieves, Braulio Robledo Delgado
 */
public class CardPlayerAdapter implements ICardPlayer {
    private final GameModel gameModel;
    private final boolean isUser;

    public CardPlayerAdapter(GameModel gameModel, boolean isUser) {
        this.gameModel = gameModel;
        this.isUser = isUser;
    }

    @Override
    public void playTurn() throws InvalidCardPlayException, DeckEmptyException {
        if (isUser) {
            // El turno del usuario es manejado por eventos UI
        } else {
            UnoCard card = gameModel.playCard(null, false);
            // Lógica para CPU - ya implementada en playCpuTurn()
        }
    }

    @Override
    public UnoCard drawCard() throws DeckEmptyException {
        return gameModel.drawCard();
    }

    @Override
    public boolean canPlayCard() {
        return isUser ? gameModel.canUserPlay() :
                gameModel.canPlayAnyCard(gameModel.getCpuHand());
    }

    @Override
    public void declareUno() {
        // Implementar lógica para declarar UNO
    }
}