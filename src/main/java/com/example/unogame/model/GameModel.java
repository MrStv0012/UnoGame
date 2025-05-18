package com.example.unogame.model;

import com.example.unogame.exceptions.DeckEmptyException;
import com.example.unogame.exceptions.GameRuleException;
import com.example.unogame.exceptions.InvalidCardPlayException;

import java.util.ArrayList;
import java.util.List;

public class GameModel {
    private final UnoDeck deck;
    private final List<UnoCard> userHand;
    private final List<UnoCard> cpuHand;
    private boolean isUserTurn;
    private UnoCard topDiscard;
    private UnoCard.Color currentColor;
    private boolean skipNextTurn; // Para manejar SKIP y DRAW_TWO correctamente

    public GameModel() throws DeckEmptyException {
        this.deck = new UnoDeck();
        this.userHand = new ArrayList<>();
        this.cpuHand = new ArrayList<>();
        this.isUserTurn = true;
        this.skipNextTurn = false;
        initializeGame();
    }

    private void initializeGame() throws DeckEmptyException {

        userHand.addAll(deck.drawCards(5));
        cpuHand.addAll(deck.drawCards(5));
        topDiscard = deck.drawCard();
        deck.addToDiscardPile(topDiscard);

        // Manejar si la primera carta es especial
        if (topDiscard.getValue() == UnoCard.Value.SKIP ||
                topDiscard.getValue() == UnoCard.Value.REVERSE) {
            // Si la primera carta es SKIP o REVERSE, la CPU pierde su turno
            isUserTurn = false;

        } else if (topDiscard.getValue() == UnoCard.Value.DRAW_TWO ||
                topDiscard.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {
            // Si es +2 o +4, el jugador humano roba cartas (se maneja en la vista)
            isUserTurn = true;
        } else if (topDiscard.getValue() == UnoCard.Value.WILD) {
            // Para cartas comodín, el jugador elige un color (se maneja en la vista)
            isUserTurn = true;
        }
    }

    public UnoCard drawCard() throws DeckEmptyException {
        if (deck.size() == 0) {
            if (deck.discardSize() <= 1) {
                throw new DeckEmptyException("No quedan cartas en el mazo ni en el descarte");
            }
            deck.refillFromDiscard();
        }
        if (deck.size() == 0) {
            throw new DeckEmptyException("No quedan cartas disponibles");
        }
        return deck.drawCard();
    }

    public void setWildColor(UnoCard.Color color) {
        this.currentColor = color;
    }

    public UnoCard.Color getCurrentColor() {
        return currentColor != null ? currentColor : topDiscard.getColor();
    }

    public boolean canUserPlay() {
        // Simplemente llama al método genérico para verificar si hay cartas jugables
        return canPlayAnyCard(userHand);
    }

    public boolean canPlayAnyCard(List<UnoCard> hand) {
        if (hand == null || hand.isEmpty()) return false;

        for (UnoCard card : hand) {
            if (isValidPlay(card)) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidPlay(UnoCard card) {
        // Si el juego ha terminado, ninguna carta es jugable
        if (isGameOver() || card == null) {
            return false;
        }

        // Si es un comodín, siempre es válido
        if (card.getColor() == UnoCard.Color.WILD) {
            return true;
        }

        // Verificar color actual (respetando los colores elegidos para comodines)
        UnoCard.Color effectiveColor = getCurrentColor();
        if (card.getColor() == effectiveColor) {
            return true;
        }

        // Verificar si los valores coinciden
        return card.getValue() == topDiscard.getValue();
    }
    /**
     * Juega una carta para el jugador especificado
     * @param card La carta a jugar (null para CPU selecciona automáticamente)
     * @param isUser true si es el usuario, false si es la CPU
     * @return La carta jugada o null si no se pudo jugar ninguna
     */
    public UnoCard playCard(UnoCard card, boolean isUser) throws InvalidCardPlayException, DeckEmptyException {
        // Evitar cambios de estado si el juego ya terminó
        if (isGameOver()) {
            throw new GameRuleException("El juego ha terminado, no se pueden jugar más cartas.");
        }

        List<UnoCard> hand = isUser ? userHand : cpuHand;

        // Si no se proporciona carta (CPU), seleccionar la primera válida
        if (card == null && !isUser) {
            for (UnoCard cpuCard : hand) {
                if (isValidPlay(cpuCard)) {
                    card = cpuCard;
                    break;
                }
            }
            // No se encontró carta válida
            if (card == null) return null;
        }

        // Verificar que la carta sea válida
        if (!isValidPlay(card)) {
            throw new InvalidCardPlayException("La carta seleccionada no es válida para jugar.");
        }

        // Verificar que la carta esté en la mano del jugador
        if (!hand.contains(card)) {
            throw new InvalidCardPlayException("La carta seleccionada no está en tu mano.");
        }

        // Jugar la carta
        hand.remove(card);
        try {
            deck.addToDiscardPile(card);
        } catch (Exception e) {
            // Si ocurre un error, devolver la carta a la mano y propagar la excepción
            hand.add(card);
            throw new GameRuleException("Error al agregar carta al descarte: " + e.getMessage());
        }
        topDiscard = card;

        // Manejar color para comodines
        if (card.getColor() != UnoCard.Color.WILD) {
            currentColor = null;
        } else if (!isUser) {
            // Solo para CPU: elegir automáticamente el mejor color
            currentColor = chooseBestColorForCpu();
        }
        // Para el usuario, el color se establece mediante setWildColor()

        // Aplicar efectos especiales
        applyCardEffect(card, !isUser);

        // Manejar turnos
        if (!skipNextTurn) {
            switchTurn();
        } else {
            skipNextTurn = false;
        }

        return card;
    }
    
    private UnoCard.Color chooseBestColorForCpu() {
        int[] colorCount = new int[4]; // RED, BLUE, GREEN, YELLOW

        for (UnoCard card : cpuHand) {
            if (card.getColor() != UnoCard.Color.WILD) {
                colorCount[card.getColor().ordinal()]++;
            }
        }

        // Encontrar el color más común
        int maxIndex = 0;
        for (int i = 1; i < 4; i++) {
            if (colorCount[i] > colorCount[maxIndex]) {
                maxIndex = i;
            }
        }

        // Si no hay preferencia, elegir RED por defecto
        return colorCount[maxIndex] > 0 ?
                UnoCard.Color.values()[maxIndex] : UnoCard.Color.RED;
    }

    public void applyCardEffect(UnoCard card, boolean isCpuTurn) throws DeckEmptyException {
        new CardEffectHandler().applyEffect(card, isCpuTurn);
    }

    private class CardEffectHandler {
        public void applyEffect(UnoCard card, boolean isCpuTurn) throws DeckEmptyException {
            switch (card.getValue()) {
                case DRAW_TWO:
                    for (int i = 0; i < 2; i++) {
                        if (isCpuTurn) {
                            drawUserCard();
                        } else {
                            drawCpuCard();
                        }
                    }
                    skipNextTurn = true;
                    break;
                case WILD_DRAW_FOUR:
                    for (int i = 0; i < 4; i++) {
                        if (isCpuTurn) {
                            drawUserCard();
                        } else {
                            drawCpuCard();
                        }
                    }
                    skipNextTurn = true;
                    break;
                case SKIP:
                case REVERSE:
                    // Para SKIP y REVERSE, simplemente saltamos el siguiente turno
                    skipNextTurn = true;
                    break;
                default:
                    break;
            }
        }
    }

    public UnoCard drawUserCard() throws DeckEmptyException {
        UnoCard card = drawCard();
        userHand.add(card);
        return card;
    }

    public UnoCard drawCpuCard() throws DeckEmptyException {
        UnoCard card = drawCard();
        cpuHand.add(card);
        return card;
    }

    public void switchTurn() {
        new TurnManager().switchTurn();
    }

    public void setUserTurn(boolean isUserTurn) {
        this.isUserTurn = isUserTurn;
    }

    private class TurnManager {
        public void switchTurn() {
            isUserTurn = !isUserTurn;
        }

        public boolean isSkipNextTurn() {
            return skipNextTurn;
        }

        public void setSkipNextTurn(boolean skip) {
            skipNextTurn = skip;
        }
    }

    // --- Getters ---
    public List<UnoCard> getUserHand() { return userHand; }
    public List<UnoCard> getCpuHand() { return cpuHand; }
    public UnoCard getTopDiscard() { return topDiscard; }
    public boolean isUserTurn() { return isUserTurn; }

    // Métodos para comprobar si el juego ha terminado
    public boolean isGameOver() {
        return userHand.isEmpty() || cpuHand.isEmpty();
    }

    public boolean userWins() {
        return userHand.isEmpty();
    }
}