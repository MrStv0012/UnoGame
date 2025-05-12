package com.example.unogame.controller;

import com.example.unogame.model.GameModel;
import com.example.unogame.model.UnoCard;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.application.Platform;

import java.util.Objects;
import java.util.Optional;

public class GameViewController {
    @FXML private HBox cpuHand;
    @FXML private HBox userHand;
    @FXML private StackPane centerPane;
    @FXML private ImageView deckView;
    @FXML private ImageView discardPileView;
    @FXML private Button unoButton;
    @FXML private Button drawCardButton; // Necesitarás agregar este botón en tu FXML o ajustar el código

    private GameModel gameModel;
    private final String prefix= "/com/example/unogame/cards-uno/";
    private boolean gameOver = false;

    @FXML
    private void initialize() {
        gameModel = new GameModel();
        setupDeckImage();
        dealInitialHands();
        setupUnoButton();
        setupDrawCardButton();

        // Manejar carta inicial especial
        handleInitialCard();
    }

    private void handleInitialCard() {
        UnoCard topCard = gameModel.getTopDiscard();

        // Si la primera carta es un comodín, el jugador elige color
        if (topCard.getValue() == UnoCard.Value.WILD ||
                topCard.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {

            // Retrasar para que se muestre después de inicializar la interfaz
            Platform.runLater(() -> {
                promptColorChoice();

                // Si es +4, aplicar efecto (el jugador roba 4)
                if (topCard.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {
                    applyInitialDraw(4);
                }
            });
        }
        // Si es otra carta especial, aplicar su efecto
        else if (topCard.getValue() == UnoCard.Value.DRAW_TWO) {
            applyInitialDraw(2);
        }

        if (!gameModel.isUserTurn()) {
            // Opción: informar al usuario
            Platform.runLater(() -> {
                showAlertSafely("Turno inicial saltado",
                        "Tu turno ha sido saltado por " + topCard.getValue(),
                        Alert.AlertType.INFORMATION);

                // Después de un breve retraso, inicia la CPU
                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(e -> playCpuTurn());
                pause.play();
            });
        }
    }

    private void applyInitialDraw(int count) {
        // Si la carta inicial es un +2 o +4, el jugador roba cartas
        for (int i = 0; i < count; i++) {
            UnoCard drawnCard = gameModel.drawCard(true);
            ImageView cardView = createCardImage(drawnCard, false);
            userHand.getChildren().add(cardView);
        }

        Platform.runLater(() -> {
            showAlertSafely("Carta inicial",
                    "Has robado " + count + " cartas por la carta inicial.",
                    Alert.AlertType.INFORMATION);
        });
    }

    private void setupDeckImage() {
        deckView.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream(prefix + "deck_of_cards.png")
        )));

        // Agregar evento de clic al mazo para robar carta
        deckView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (gameModel.isUserTurn() && !gameOver) {
                handleDrawCard();
            }
        });
    }

    private void setupDrawCardButton() {
        // Si tienes un botón separado para robar cartas
        if (drawCardButton != null) {
            drawCardButton.setOnAction(event -> {
                if (gameModel.isUserTurn() && !gameOver) {
                    handleDrawCard();
                }
            });
        }
    }

    private void handleDrawCard() {
        if (!gameModel.canUserPlay()) {
            // Animar la carta robada
            UnoCard drawnCard = gameModel.drawCard(true);
            ImageView cardView = createCardImage(drawnCard, false);

            // Añadir la carta a la mano con animación
            cardView.setOpacity(0);
            userHand.getChildren().add(cardView);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), cardView);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setOnFinished(e -> {
                // Pasar al turno de la CPU
                gameModel.switchTurn();
                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(pe -> playCpuTurn());
                pause.play();
            });
            fadeIn.play();
        } else {
            // Si el usuario puede jugar una carta, mostrar mensaje
            showAlertSafely("Tienes una carta jugable",
                    "Debes jugar una carta que coincida con el color o valor.",
                    Alert.AlertType.INFORMATION);
        }
    }

    private void dealInitialHands() {
        // Limpiar las manos
        userHand.getChildren().clear();
        cpuHand.getChildren().clear();

        // Mostrar cartas del usuario (boca arriba)
        for (UnoCard card : gameModel.getUserHand()) {
            ImageView cardView = createCardImage(card, false);
            userHand.getChildren().add(cardView);
        }

        // Mostrar cartas de la CPU (boca abajo)
        for (int i = 0; i < gameModel.getCpuHand().size(); i++) {
            ImageView cardBack = new ImageView(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream(prefix + "card_uno.png")
            )));
            cardBack.setFitWidth(80);
            cardBack.setPreserveRatio(true);
            cpuHand.getChildren().add(cardBack);
        }

        // Mostrar primera carta en el descarte
        updateDiscardPile();
    }

    private ImageView createCardImage(UnoCard card, boolean isCpu) {
        String imagePath = prefix + card.toFileName();
        ImageView imageView = new ImageView(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream(imagePath))
        ));

        imageView.setFitWidth(80);
        imageView.setPreserveRatio(true);

        if (!isCpu) {
            imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleCardClick);
        }

        // Añadir un tooltip o identificador para depuración
        imageView.setUserData(card);

        return imageView;
    }

    private void handleCardClick(MouseEvent event) {
        if (!gameModel.isUserTurn() || gameOver) return;

        ImageView clickedCard = (ImageView) event.getSource();
        int cardIndex = userHand.getChildren().indexOf(clickedCard);

        if (cardIndex < 0 || cardIndex >= gameModel.getUserHand().size()) {
            return; // Protección contra índices fuera de rango
        }

        UnoCard selectedCard = gameModel.getUserHand().get(cardIndex);

        if (gameModel.isValidPlay(selectedCard)) {
            final UnoCard cardToPlay = selectedCard; // Para la lambda

            animateCardPlay(clickedCard, () -> {
                gameModel.playUserCard(cardToPlay);
                userHand.getChildren().remove(clickedCard);
                updateDiscardPile();

                // Fix: Handle special cards properly
                if (cardToPlay.getValue() == UnoCard.Value.DRAW_TWO) {
                    updateCpuHandAfterDraw();

                    checkGameOver();
                    if (gameOver) return;

                    checkUno();
                    return;
                }

                if (cardToPlay.getValue() == UnoCard.Value.WILD ||
                        cardToPlay.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {

                    // Fix: Handle color choice safely with proper sequencing
                    Platform.runLater(() -> {
                        promptColorChoice();

                        if (cardToPlay.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {
                            // CPU draws 4 cards
                            updateCpuHandAfterDraw();

                            // Fix: La CPU pierde su turno después de un +4
                            // No se invoca playCpuTurn() porque la CPU debe perder el turno
                            showAlertSafely("CPU pierde turno",
                                    "La CPU ha robado 4 cartas y pierde su turno.",
                                    Alert.AlertType.INFORMATION);
                        } else {
                            // Solo para WILD (no +4), programar el turno de la CPU
                            PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
                            delay.setOnFinished(e -> {
                                checkGameOver();
                                if (!gameOver) {
                                    playCpuTurn();
                                }
                            });
                            delay.play();
                        }

                        checkUno();
                    });
                    return;
                }

                if (cardToPlay.getValue() == UnoCard.Value.SKIP ||
                        cardToPlay.getValue() == UnoCard.Value.REVERSE) {
                    // Fix: For SKIP and REVERSE, the CPU's turn is skipped
                    checkGameOver();
                    if (gameOver) return;

                    checkUno();
                    return;
                }

                // Fix: For normal cards (not special), play CPU turn
                checkGameOver();
                if (gameOver) return;

                playCpuTurn();
                checkUno();
            });
        } else {
            shakeAnimation(clickedCard);
        }
    }
    private void updateCpuHandAfterDraw() {
        // Actualizar visualmente la mano de la CPU después de que robe cartas
        int previousSize = cpuHand.getChildren().size();
        int currentSize = gameModel.getCpuHand().size();
        int difference = currentSize - previousSize;

        // Agregar nuevas cartas visuales si la CPU ha robado
        for (int i = previousSize; i < currentSize ; i++) {
            ImageView newCard = new ImageView(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream(prefix + "card_uno.png")
            )));
            newCard.setFitWidth(80);
            newCard.setPreserveRatio(true);
            newCard.setOpacity(0);

            cpuHand.getChildren().add(newCard);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newCard);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            Platform.runLater(() -> {
                showAlertSafely("CPU roba cartas",
                        "La CPU ha robado " + difference + " cartas.",
                        Alert.AlertType.INFORMATION);
            });
        }
    }

    private void promptColorChoice() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Elige un color");
        alert.setHeaderText("Selecciona el nuevo color:");

        // Botones para cada color
        ButtonType redButton = new ButtonType("Rojo");
        ButtonType blueButton = new ButtonType("Azul");
        ButtonType greenButton = new ButtonType("Verde");
        ButtonType yellowButton = new ButtonType("Amarillo");
        alert.getButtonTypes().setAll(redButton, blueButton, greenButton, yellowButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            ButtonType buttonType = result.get();
            if (buttonType == redButton) {
                gameModel.setWildColor(UnoCard.Color.RED);
            } else if (buttonType == blueButton) {
                gameModel.setWildColor(UnoCard.Color.BLUE);
            } else if (buttonType == greenButton) {
                gameModel.setWildColor(UnoCard.Color.GREEN);
            } else if (buttonType == yellowButton) {
                gameModel.setWildColor(UnoCard.Color.YELLOW);
            }
        } else {
            // Si no se elige un color, establecer uno por defecto
            gameModel.setWildColor(UnoCard.Color.RED);
        }
    }

    private void animateCardPlay(ImageView card, Runnable onFinished) {
        TranslateTransition move = new TranslateTransition(Duration.millis(300), card);
        move.setToX(discardPileView.getLayoutX() - card.getLayoutX());
        move.setToY(discardPileView.getLayoutY() - card.getLayoutY());
        move.setOnFinished(e -> onFinished.run());
        move.play();
    }

    private void playCpuTurn() {
        if (gameOver) return;

        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> {
            UnoCard playedCard = gameModel.playCpuCard();
            if (playedCard != null) {
                // La CPU juega una carta
                if (cpuHand.getChildren().isEmpty()) return;

                ImageView cpuCard = (ImageView) cpuHand.getChildren().get(0);
                animateCardPlay(cpuCard, () -> {
                    cpuHand.getChildren().remove(0);
                    updateDiscardPile();

                    // Verificar si la CPU ganó
                    checkGameOver();
                    if (gameOver) return;

                    // Si la carta era un comodín, mostrar el color elegido
                    if ((playedCard.getValue() == UnoCard.Value.WILD ||
                            playedCard.getValue() == UnoCard.Value.WILD_DRAW_FOUR) &&
                            gameModel.getCurrentColor() != null) {

                        Platform.runLater(() -> {
                            showAlertSafely("CPU eligió color",
                                    "La CPU eligió el color: " + gameModel.getCurrentColor().name(),
                                    Alert.AlertType.INFORMATION);

                            // Si se jugó +4, actualizar visualmente la mano del usuario
                            if (playedCard.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {
                                updateUserHandAfterDraw(4);
                                playCpuTurn();
                            }
                        });
                    }
                    // Si jugó +2, actualizar mano del usuario
                    else if (playedCard.getValue() == UnoCard.Value.DRAW_TWO) {
                        updateUserHandAfterDraw(2);
                        playCpuTurn();
                    }

                    else if(playedCard.getValue() == UnoCard.Value.REVERSE ||
                            playedCard.getValue() == UnoCard.Value.SKIP) {
                        playCpuTurn();
                    }
                });
            } else {
                // La CPU roba una carta
                UnoCard drawnCard = gameModel.drawCard(false);

                // Crear una nueva carta boca abajo para la CPU
                ImageView newCard = new ImageView(new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream(prefix + "card_uno.png")
                )));
                newCard.setFitWidth(80);
                newCard.setPreserveRatio(true);

                // Animación para la carta robada
                newCard.setOpacity(0);
                cpuHand.getChildren().add(newCard);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), newCard);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.setOnFinished(animEvent -> {
                    // Después de robar, cambiar al turno del usuario
                    gameModel.switchTurn();
                });
                fadeIn.play();
            }
        });
        delay.play();
    }

    private void updateUserHandAfterDraw(int count) {
        // Mostrar visualmente cuando el usuario debe robar después de un +2 o +4
        for (int i = 0; i < count; i++) {
            UnoCard drawnCard = gameModel.getUserHand().get(gameModel.getUserHand().size() - count + i);
            ImageView cardView = createCardImage(drawnCard, false);
            cardView.setOpacity(0);

            userHand.getChildren().add(cardView);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), cardView);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        }

        // Informar al usuario
        showAlertSafely("Robaste cartas",
                "La CPU te ha hecho robar " + count + " cartas.",
                Alert.AlertType.INFORMATION);
    }

    private void updateDiscardPile() {
        UnoCard topCard = gameModel.getTopDiscard();
        String imagePath = prefix + topCard.toFileName();

        try {
            discardPileView.setImage(new Image(
                    Objects.requireNonNull(getClass().getResourceAsStream(imagePath))
            ));
        } catch (Exception e) {
            System.err.println("Error al cargar imagen: " + imagePath);
            e.printStackTrace();
        }

        // Opcional: mostrar una indicación visual del color actual si es un comodín
        // Esto podría implementarse con un círculo de color o similar
    }

    private void checkUno() {
        if (gameModel.getUserHand().size() == 1) {
            unoButton.setVisible(true);
            // Temporizador para penalizar si no se presiona UNO
            PauseTransition timer = new PauseTransition(Duration.seconds(3));
            timer.setOnFinished(e -> {
                if (unoButton.isVisible()) {
                    // Penalización: robar 2 cartas
                    for (int i = 0; i < 2; i++) {
                        UnoCard penaltyCard = gameModel.drawCard(true);
                        ImageView cardView = createCardImage(penaltyCard, false);
                        userHand.getChildren().add(cardView);
                    }
                    unoButton.setVisible(false);

                    Platform.runLater(() -> {
                        showAlertSafely("¡Olvidaste decir UNO!",
                                "Has recibido 2 cartas de penalización.",
                                Alert.AlertType.WARNING);
                    });
                }
            });
            timer.play();
        }
    }

    private void setupUnoButton() {
        unoButton.setVisible(false);
        unoButton.setOnAction(e -> {
            unoButton.setVisible(false);
            // Animación o efecto al presionar UNO
            showAlertSafely("¡UNO!", "¡Has dicho UNO a tiempo!", Alert.AlertType.INFORMATION);
        });
    }

    private void checkGameOver() {
        if (gameModel.isGameOver()) {
            gameOver = true;
            String winner = gameModel.userWins() ? "¡Has ganado!" : "La CPU ha ganado";

            // Usar Platform.runLater para mostrar el diálogo de manera segura
            Platform.runLater(() -> {
                showAlertSafely("Fin del juego", winner, Alert.AlertType.INFORMATION);
            });
        }
    }

    // Método seguro para mostrar alertas que evita el problema de showAndWait durante animaciones
    private void showAlertSafely(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> alert.hide());
        delay.play();
    }

    private void shakeAnimation(ImageView card) {
        // Animación de "error" al jugar carta inválida
        TranslateTransition shake = new TranslateTransition(Duration.millis(100), card);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }
}