package com.example.unogame.controller;

import com.example.unogame.model.UnoCard;
import com.example.unogame.model.UnoDeck;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;


import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GameViewController {
    @FXML private HBox cpuHand;
    @FXML private HBox userHand;
    @FXML private StackPane centerPane;
    @FXML private ImageView deckView;
    @FXML private ImageView discardPileView;
    @FXML private Button unoButton;

    private UnoDeck deck;

    private boolean isUserTurn = true;

    @FXML
    private void initialize() {
        String prefix = "/com/example/unogame/cards-uno/";

        // Inicializar mazo
        deck = new UnoDeck();

        // Configurar imágen del mazo
        deckView.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(prefix + "deck_of_cards.png")
        )));

        // Repartir cartas iniciales
        dealInitialHands(prefix);

        // Poner primera carta en descarte
        UnoCard firstCard = deck.drawCard();
        deck.addToDiscardPile(firstCard);
        updateDiscardPile();

        // Configurar botón UNO
        unoButton.setVisible(false);
        unoButton.setOnAction(e -> handleUno());
    }

    private void dealInitialHands(String prefix) {
        // Repartir 5 cartas al usuario
        List<UnoCard> userCards = deck.drawCards(5);
        List<String> userPaths = userCards.stream()
                .map(UnoCard::toFileName)
                .map(fn -> prefix + fn)
                .collect(Collectors.toList());
        dealCardsToHand(userPaths, userHand, false);

        // Repartir 5 cartas a la CPU (boca abajo)
        List<String> cpuPaths = userCards.stream()
                .map(c -> prefix + "card_uno.png")
                .collect(Collectors.toList());
        dealCardsToHand(cpuPaths, cpuHand, true);
    }

    private void dealCardsToHand(List<String> cardPaths, HBox hand, boolean isCpu) {
        SequentialTransition seq = new SequentialTransition();

        for (int i = 0; i < cardPaths.size(); i++) {
            String path = cardPaths.get(i);
            ImageView card = createCardImage(path, isCpu);
            hand.getChildren().add(card);


            setupDealAnimation(card, i, seq);
        }

        seq.play();
    }

    private ImageView createCardImage(String resourcePath, boolean isCpu) {
        ImageView iv = new ImageView(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream(resourcePath))
        ));
        iv.setFitWidth(80);
        iv.setPreserveRatio(true);
        iv.getStyleClass().add("card-image");

        if (!isCpu) {
            iv.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleCardClick);
        }

        return iv;
    }

    private void handleCardClick(MouseEvent event) {
        if (!isUserTurn) return;

        ImageView clickedCard = (ImageView) event.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(150), clickedCard);
        st.setToX(1.1);
        st.setToY(1.1);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();

        // Aquí puee ir la lógica para validar y jugar la carta
    }

    private void updateDiscardPile() {
        try {
            UnoCard topCard = deck.getTopDiscard();
            String imagePath = "/com/example/unogame/cards-uno/" + topCard.toFileName();
            discardPileView.setImage(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream(imagePath))
            ));
        } catch (IllegalStateException e) {
        }
    }

    private void handleUno() {
        unoButton.setVisible(false);
        // Despues añadir la ógica para verificar UNO
    }

    private void setupDealAnimation(ImageView card, int index, SequentialTransition seq) {
        card.setOpacity(0);
        card.setTranslateX(centerPane.getWidth() / 2 - 50);
        card.setTranslateY(centerPane.getHeight() / 2 - 70);

        TranslateTransition move = new TranslateTransition(Duration.millis(300), card);
        move.setToX(0);
        move.setToY(0);

        FadeTransition fade = new FadeTransition(Duration.millis(300), card);
        fade.setFromValue(0);
        fade.setToValue(1);

        ParallelTransition pt = new ParallelTransition(move, fade);
        pt.setInterpolator(Interpolator.EASE_OUT);
        pt.setDelay(Duration.millis(index * 100));
        seq.getChildren().add(pt);
    }
}
