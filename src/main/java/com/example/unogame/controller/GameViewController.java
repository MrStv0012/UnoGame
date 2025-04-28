package com.example.unogame;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;

public class GameViewController {
    @FXML private HBox cpuHand;
    @FXML private HBox userHand;
    @FXML private StackPane centerPane;
    @FXML private ImageView deckView;
    @FXML private ImageView discardPileView;

    @FXML
    private void initialize() {
        String prefix = "/com/example/unogame/cards-uno/";
        String back = "wild.png" ;  // ajusta al nombre del reverso que quieras usar

        // Carga inicial de mazo y descarte
        deckView.setImage(new Image(
                getClass().getResourceAsStream(prefix + back)
        ));
        discardPileView.setImage(new Image(
                getClass().getResourceAsStream(prefix + back)
        ));

        // Ejemplo: repartir 5 cartas al usuario con animación
        dealInitialCards(List.of(
                prefix + back,
                prefix + back,
                prefix + back,
                prefix + back,
                prefix + back
        ));

        dealInitialCardsCpu(List.of(
                prefix + "card_uno.png",
                prefix + "card_uno.png",
                prefix + "card_uno.png",
                prefix + "card_uno.png",
                prefix + "card_uno.png"
        ));
    }


    /**
     * Anima el reparto de N cartas desde el mazo al HBox userHand.
     */
    public void dealInitialCards(List<String> cardPaths) {
        double totalWidth = cardPaths.size() * 60;
        double startX = -totalWidth / 2 + 30;
        SequentialTransition seq = new SequentialTransition();

        for (int i = 0; i < cardPaths.size(); i++) {
            String path = cardPaths.get(i);
            ImageView card = makeCardImage(path);
            card.setOpacity(0);
            userHand.getChildren().add(card);


            // Posición inicial: encima del mazo
            card.setTranslateX(centerPane.getWidth() / 2 - 50);
            card.setTranslateY(centerPane.getHeight() / 2 - 70);

            // Movimiento + fade-in
            TranslateTransition move = new TranslateTransition(Duration.millis(300), card);
            move.setToX(startX + i * 60);
            move.setToY(0);

            FadeTransition fade = new FadeTransition(Duration.millis(300), card);
            fade.setFromValue(0);
            fade.setToValue(1);

            ParallelTransition pt = new ParallelTransition(move, fade);
            pt.setInterpolator(Interpolator.EASE_OUT);
            pt.setDelay(Duration.millis(i * 100));
            seq.getChildren().add(pt);
        }

        seq.play();
    }

    public void dealInitialCardsCpu(List<String> cardPaths) {
        double totalWidth = cardPaths.size() * 60;
        double startX = -totalWidth / 2 + 30;
        SequentialTransition seq = new SequentialTransition();

        for (int i = 0; i < cardPaths.size(); i++) {
            String path = cardPaths.get(i);
            ImageView card = makeCardImage(path);
            card.setOpacity(0);
            cpuHand.getChildren().add(card);


            // Posición inicial: encima del mazo
            card.setTranslateX(centerPane.getWidth() / 2 - 50);
            card.setTranslateY(centerPane.getHeight() / 2 - 70);

            // Movimiento + fade-in
            TranslateTransition move = new TranslateTransition(Duration.millis(300), card);
            move.setToX(startX + i * 60);
            move.setToY(0);

            FadeTransition fade = new FadeTransition(Duration.millis(300), card);
            fade.setFromValue(0);
            fade.setToValue(1);

            ParallelTransition pt = new ParallelTransition(move, fade);
            pt.setInterpolator(Interpolator.EASE_OUT);
            pt.setDelay(Duration.millis(i * 100));
            seq.getChildren().add(pt);
        }

        seq.play();
    }

    /** Crea un ImageView para una carta con efecto “pop” al clic */
    private ImageView makeCardImage(String resourcePath) {
        ImageView iv = new ImageView(new Image(
                getClass().getResourceAsStream(resourcePath)
        ));
        iv.setFitWidth(80);
        iv.setPreserveRatio(true);
        iv.getStyleClass().add("card-image");

        iv.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), iv);
            st.setToX(1.1);
            st.setToY(1.1);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
            // aquí luego conectarás controller.onUserPlays(...)
        });

        return iv;
    }

    /** Llama a este método desde tu GameController cuando cambie la pila de descartes */
    /**
    public void updateDiscardPile(String cardPath) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), discardPileView);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            discardPileView.setImage(new Image(
                    getClass().getResourceAsStream(cardPath)
            ));
            new FadeTransition(Duration.millis(200), discardPileView) {{
                setToValue(1);
                play();
            }};
        });
        fadeOut.play();
    }
     */
}
