package com.example.unogame.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class UnoDeck {
    private final List<UnoCard> cards = new ArrayList<>();
    private List<UnoCard> discardPile = new ArrayList<>();
    private final Random rng = new Random();

    public UnoDeck() {
        reset();
        shuffle();
    }

    public void reset() {
        cards.clear();

        // Cartas normales (0-9) por color
        for (UnoCard.Color color : UnoCard.Color.values()) {
            if (color == UnoCard.Color.WILD) continue;

            // Un solo cero por color
            cards.add(new UnoCard(color, UnoCard.Value.ZERO));

            // Una de cada número del 1-9 por color
            for (int i = 1; i <= 9; i++) {
                cards.add(new UnoCard(color, UnoCard.Value.getValue(i)));
            }
            for (int i = 1; i <= 2; i++) {
                // Cartas especiales por color
                cards.add(new UnoCard(color, UnoCard.Value.SKIP));
                cards.add(new UnoCard(color, UnoCard.Value.REVERSE));
                cards.add(new UnoCard(color, UnoCard.Value.DRAW_TWO));
            }
        }

        // Comodines
        for (int i = 0; i < 4; i++) {
            cards.add(new UnoCard(UnoCard.Color.WILD, UnoCard.Value.WILD));
            cards.add(new UnoCard(UnoCard.Color.WILD, UnoCard.Value.WILD_DRAW_FOUR));
        }
    }


    public void shuffle() {
        Collections.shuffle(cards, rng);
    }


    public UnoCard drawCard() {
        if (cards.isEmpty()) {
            refillFromDiscard();

        }
        return cards.remove(cards.size() - 1);
    }


    public List<UnoCard> drawCards(int n) {
        if (n < 0 || n > cards.size()) {
            throw new IllegalArgumentException("No se pueden robar " + n + " cartas");

        }
        List<UnoCard> hand = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            hand.add(drawCard());
        }
        return hand;
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
        if (discardPile.size() > 1) {

            UnoCard lastCard = discardPile.remove(discardPile.size() - 1);

            cards.addAll(discardPile);
            discardPile.clear();
            discardPile.add(lastCard);

            shuffle();
        } else {
            throw new IllegalStateException("No hay cartas disponibles");
        }
    }

    public int size() {
        return cards.size();
    }
}

