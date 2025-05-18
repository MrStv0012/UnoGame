package com.example.unogame.view;

import com.example.unogame.model.UnoCard;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.util.Objects;

public class GameView {
    private final ImageView discardPileView;
    private final String cardPrefix = "/com/example/unogame/cards-uno/";
    private boolean gameOver = false;

    public GameView(ImageView discardPileView) {
        this.discardPileView = discardPileView;
    }

    public void showAlertSafely(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> alert.hide());
        delay.play();
    }

    public void shakeAnimation(ImageView card) {
        TranslateTransition shake = new TranslateTransition(Duration.millis(100), card);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }

    public void animateCardPlay(ImageView card, Runnable onFinished) {
        TranslateTransition move = new TranslateTransition(Duration.millis(300), card);
        move.setToX(discardPileView.getLayoutX() - card.getLayoutX());
        move.setToY(discardPileView.getLayoutY() - card.getLayoutY());
        move.setOnFinished(e -> onFinished.run());
        move.play();
    }

    public void updateDiscardPile(UnoCard topCard) {
        String imagePath = cardPrefix + topCard.toFileName();

        try {
            discardPileView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath))));
        } catch (Exception e) {
            System.err.println("Error al cargar imagen: " + imagePath);
            e.printStackTrace();
        }
    }

    public boolean checkGameOver(boolean isGameOver, boolean userWins) {
        if (isGameOver && !gameOver) {
            gameOver = true;
            String winner = userWins ? "¡Has ganado!" : "La CPU ha ganado";

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Fin del juego");
                alert.setHeaderText(null);
                alert.setContentText(winner);
                alert.showAndWait();
            });
        }
        return gameOver;
    }

    public void animateFadeIn(ImageView card, Duration duration, Runnable onFinished) {
        card.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(duration, card);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(e -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
        fadeIn.play();
    }

    public void showCpuColorChoice(UnoCard.Color color) {
        Platform.runLater(() -> {
            showAlertSafely("CPU eligió color", "La CPU eligió el color: " + color.name(), Alert.AlertType.INFORMATION);
        });
    }

    public void showDrawCardsMessage(String message, boolean isInfo) {
        Platform.runLater(() -> {
            showAlertSafely(isInfo ? "Cartas robadas" : "Efecto de carta", message, isInfo ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        });
    }
}