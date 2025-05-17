package com.example.unogame.model;

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

    public GameModel() {
        this.deck = new UnoDeck();
        this.userHand = new ArrayList<>();
        this.cpuHand = new ArrayList<>();
        this.isUserTurn = true;
        this.skipNextTurn = false;
        initializeGame();
    }

    private void initializeGame() {

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

    public UnoCard drawCard(boolean isUser) {
        UnoCard drawnCard = deck.drawCard(); // UnoDeck ya maneja si hay que recargar
        if (isUser) {
            userHand.add(drawnCard);
        } else {
            cpuHand.add(drawnCard);
        }
        return drawnCard;
    }

    public void setWildColor(UnoCard.Color color) {
        this.currentColor = color;
    }

    public UnoCard.Color getCurrentColor() {
        return currentColor != null ? currentColor : topDiscard.getColor();
    }

    // --- Lógica principal ---




    public boolean canUserPlay() {
        return canPlayAnyCard(userHand);
    }

    public boolean canPlayAnyCard(List<UnoCard> hand) {
        for (UnoCard card : hand) {
            if (isValidPlay(card)) {
                return true;
            }
        }
        return false;
    }
    public boolean isValidPlay(UnoCard card) {
        // Si el juego ha terminado, ninguna carta es jugable
        if (isGameOver()) {
            return false;
        }

        // Si es un comodín, siempre es válido
        if (card.getColor() == UnoCard.Color.WILD) {
            return true;
        }

        // Verificar color actual (respetando los colores elegidos para comodines)
        if (card.getColor() == getCurrentColor()) {
            return true;
        }

        // Verificar si los valores coinciden
        return card.getValue() == topDiscard.getValue();
    }

    public void playUserCard(UnoCard card) {
        if (isValidPlay(card)) {
            userHand.remove(card);
            deck.addToDiscardPile(card);
            topDiscard = card;

            // Restablecer el color actual si no es una carta comodín
            if (card.getColor() != UnoCard.Color.WILD) {
                currentColor = null;
            }

            applyCardEffect(card, false);

            // Solo cambiamos el turno si no se debe saltar
            if (!skipNextTurn) {
                switchTurn();
            } else {
                skipNextTurn = false; // Restablecer la bandera
            }
        }
    }

    public UnoCard playCpuCard() {
        for (UnoCard card : cpuHand) {
            if (isValidPlay(card)) {
                cpuHand.remove(card);
                deck.addToDiscardPile(card);
                topDiscard = card;

                // Restablecer el color actual si no es una carta comodín
                if (card.getColor() != UnoCard.Color.WILD) {
                    currentColor = null;
                } else {
                    // La CPU elige un color (simple: elegir el más común en su mano)
                    currentColor = chooseBestColorForCpu();
                }

                applyCardEffect(card, true);

                // Solo cambiamos el turno si no se debe saltar
                if (!skipNextTurn) {
                    switchTurn();
                } else {
                    skipNextTurn = false; // Restablecer la bandera
                }

                return card;
            }
        }
        return null; // No hay carta válida
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

    public void applyCardEffect(UnoCard card, boolean isCpuTurn) {
        switch (card.getValue()) {
            case DRAW_TWO:
                drawCardsForOpponent(2);
                skipNextTurn = true; // El oponente pierde su turno
                break;
            case WILD_DRAW_FOUR:
                drawCardsForOpponent(4);
                skipNextTurn = true; // El oponente pierde su turno
                // El color será elegido después
                break;
            case SKIP:
            case REVERSE:
                skipNextTurn = true; // El oponente pierde su turno
                break;

            case WILD:
                // El color será elegido después
                break;
            default:
                // Cartas normales no tienen efecto especial
                break;
        }
    }

    public void drawCardsForOpponent(int count) {
        if (isUserTurn) {
            // Si es turno del usuario, la CPU roba cartas
            for (int i = 0; i < count; i++) {
                cpuHand.add(deck.drawCard());
            }
        } else {
            // Si es turno de la CPU, el usuario roba cartas
            for (int i = 0; i < count; i++) {
                userHand.add(deck.drawCard());
            }
        }
    }

    public void switchTurn() {
        isUserTurn = !isUserTurn;
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