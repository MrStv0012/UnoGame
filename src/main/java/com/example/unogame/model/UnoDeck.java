package com.example.unogame.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class UnoDeck {
    private final List<UnoCard> cards = new ArrayList<>();
    private final List<UnoCard> discardPile = new ArrayList<>();

    public UnoDeck() {
        initializeDeck();
        shuffle();
    }

    private void initializeDeck() {
        // Cartas normales por color
        for (UnoCard.Color color : UnoCard.Color.values()) {
            if (color == UnoCard.Color.WILD) continue;

            // Un solo cero y dos de cada número 1-9
            cards.add(new UnoCard(color, UnoCard.Value.ZERO));
            for (int i = 1; i <= 9; i++) {
                for (int j = 0; j < 2; j++) {
                    cards.add(new UnoCard(color, UnoCard.Value.values()[i]));
                }
            }

            // Dos de cada carta especial por color
            addSpecialCards(color, 2);
        }

        // Comodines (4 de cada tipo)
        addWildCards(4);
    }

    private void addSpecialCards(UnoCard.Color color, int count) {
        UnoCard.Value[] specials = {UnoCard.Value.SKIP, UnoCard.Value.REVERSE, UnoCard.Value.DRAW_TWO};
        for (UnoCard.Value special : specials) {
            for (int i = 0; i < count; i++) {
                cards.add(new UnoCard(color, special));
            }
        }
    }

    private void addWildCards(int count) {
        for (int i = 0; i < count; i++) {
            cards.add(new UnoCard(UnoCard.Color.WILD, UnoCard.Value.WILD));
            cards.add(new UnoCard(UnoCard.Color.WILD, UnoCard.Value.WILD_DRAW_FOUR));
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public UnoCard drawCard() {
        if (cards.isEmpty()) {
            refillFromDiscard();
        }
        return cards.remove(cards.size() - 1);
    }

    public List<UnoCard> drawCards(int count) {
        List<UnoCard> drawnCards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            drawnCards.add(drawCard());
        }
        return drawnCards;
    }

    public void addToDiscardPile(UnoCard card) {
        discardPile.add(card);
    }

    public UnoCard getTopDiscard() {
        if (discardPile.isEmpty()) {
            throw new IllegalStateException("No hay cartas en el descarte");
        }
        return discardPile.get(discardPile.size() - 1);
    }

    private void refillFromDiscard() {
        if (discardPile.size() <= 1) {
            throw new IllegalStateException("No hay suficientes cartas para continuar");
        }

        // Mantener la carta superior del descarte
        UnoCard topCard = discardPile.remove(discardPile.size() - 1);

        // Mover el resto al mazo
        cards.addAll(discardPile);
        discardPile.clear();
        discardPile.add(topCard);

        shuffle();
    }

    public int size() { return cards.size(); }
    public int discardSize() { return discardPile.size(); }
}

