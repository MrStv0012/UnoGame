package com.example.unogame.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class UnoDeck2 {
    private final List<UnoCard> cards = new ArrayList<>();
    private final Random rng = new Random();

    /**
     * Crea y baraja la baraja por primera vez.
     */
    public UnoDeck2() {
        reset();
        shuffle();
}

    public void reset() {
        cards.clear();
        // 1) Colores básicos
        for (UnoCard.Color color : UnoCard.Color.values()) {
            if (color == UnoCard.Color.WILD){
                continue;
            }
            cards.add(new UnoCard(color, UnoCard.Value.getValue(0)));

            for (int j = 1; j < 10; j++) {
                UnoCard.Value v = UnoCard.Value.getValue(j);
                cards.add(new UnoCard(color, v));
                cards.add(new UnoCard(color, v));
            }

            // Dos copias de cada especial por color
            for (UnoCard.Value special : List.of(
                    UnoCard.Value.DRAW_TWO,
                    UnoCard.Value.SKIP,
                    UnoCard.Value.REVERSE)) {
                cards.add(new UnoCard(color, special));
                cards.add(new UnoCard(color, special));
            }

            // 2) Comodines
            for (int i = 0; i < 4; i++) {
                cards.add(new UnoCard(UnoCard.Color.WILD, UnoCard.Value.WILD));
                cards.add(new UnoCard(UnoCard.Color.WILD, UnoCard.Value.WILD_DRAW_FOUR));
            }
        }
    }

    /** Baraja aleatoriamente las cartas usando Fisher–Yates. */
    public void shuffle() {
        Collections.shuffle(cards, rng);
    }

    /**
     * Roba una carta del tope de la baraja.
     * @return la carta robada
     * @throws IllegalStateException si no quedan cartas
     */
    public UnoCard drawCard() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("No hay cartas en el mazo");
        }
        return cards.remove(cards.size() - 1);
    }

    /**
     * Roba n cartas.
     * @throws IllegalArgumentException si n < 0 o n > tamaño actual
     */
    public List<UnoCard> drawCards(int n) {
        if (n < 0 || n > cards.size()) {
            throw new IllegalArgumentException("Cannot draw " + n + " cards");
        }
        List<UnoCard> hand = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            hand.add(drawCard());
        }
        return hand;
    }

    /** @return cuántas cartas quedan en el mazo */
    public int size() {
        return cards.size();
    }
}
