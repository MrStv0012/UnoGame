package com.example.unogame.controller;

import com.example.unogame.model.GameModel;
import com.example.unogame.model.UnoCard;
import com.example.unogame.view.GameView;
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
import java.util.Random;

public class GameViewController {
    @FXML private HBox cpuHand;
    @FXML private HBox userHand;
    @FXML private StackPane centerPane;
    @FXML private ImageView deckView;
    @FXML private ImageView discardPileView;
    @FXML private Button unoButton;
    @FXML private Button catchCpuButton;

    private GameModel gameModel;
    private GameView gameView;
    private final String prefix = "/com/example/unogame/cards-uno/";
    private volatile boolean gameOver = false;
    private volatile boolean userUnoClicked = false;
    private Thread userUnoThread, cpuUnoThread;
    private volatile boolean cpuDeclaredUno = false;
    private volatile boolean playerCaughtCpu = false;
    private boolean processingClick = false; // Para evitar múltiples clics rápidos

    @FXML
    private void initialize() {
        resetGame();
    }

    private void resetGame() {
        // Cancelar threads pendientes
        stopAllThreads();

        // Reiniciar flags
        gameOver = false;
        userUnoClicked = false;
        cpuDeclaredUno = false;
        playerCaughtCpu = false;
        processingClick = false;

        // Ocultar botones especiales
        if (unoButton != null) unoButton.setVisible(false);
        if (catchCpuButton != null) catchCpuButton.setVisible(false);

        // Inicializar modelo y vista
        gameModel = new GameModel();
        gameView = new GameView(discardPileView);

        // Configurar componentes UI
        setupDeckImage();
        dealInitialHands();
        setupUnoButton();
        setupCatchCpuButton();

        // Manejar carta inicial
        handleInitialCard();
    }

    private void stopAllThreads() {
        // Detener threads existentes
        if (userUnoThread != null) {
            userUnoThread.interrupt();
            userUnoThread = null;
        }

        if (cpuUnoThread != null) {
            cpuUnoThread.interrupt();
            cpuUnoThread = null;
        }
    }

    private void setupDeckImage() {
        deckView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(prefix + "deck_of_cards.png"))));

        // Agregar evento de clic al mazo para robar carta
        deckView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (gameModel.isUserTurn() && !gameOver && !processingClick) {
                handleDrawCard();
            }
        });
    }

    private void handleDrawCard() {
        if (gameOver) return;

        processingClick = true;

        if (!gameModel.canUserPlay()) {
            // Animar la carta robada
            UnoCard drawnCard = gameModel.drawCard(true);
            ImageView cardView = createCardImage(drawnCard, false);

            // Añadir la carta a la mano con animación
            userHand.getChildren().add(cardView);
            gameView.animateFadeIn(cardView, Duration.millis(500), () -> {
                // Pasar al turno de la CPU
                gameModel.switchTurn();
                processingClick = false;

                if (!gameOver) {
                    PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                    pause.setOnFinished(pe -> {
                        if (!gameOver) playCpuTurn();
                    });
                    pause.play();
                }
            });
        } else {
            processingClick = false;
            gameView.showAlertSafely("Tienes una carta jugable", "Debes jugar una carta que coincida con el color o valor.", Alert.AlertType.INFORMATION);
        }
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
            Platform.runLater(() -> {
                gameView.showAlertSafely("Turno inicial saltado", "Tu turno ha sido saltado por " + topCard.getValue(), Alert.AlertType.INFORMATION);

                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(e -> {
                    if (!gameOver) playCpuTurn();
                });
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
            gameView.showAlertSafely("Carta inicial",
                    "Has robado " + count + " cartas por la carta inicial.",
                    Alert.AlertType.INFORMATION);
        });
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
            ImageView cardBack = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(prefix + "card_uno.png"))));
            cardBack.setFitWidth(80);
            cardBack.setPreserveRatio(true);
            cpuHand.getChildren().add(cardBack);
        }
        // Mostrar primera carta en el descarte
        gameView.updateDiscardPile(gameModel.getTopDiscard());
    }

    private ImageView createCardImage(UnoCard card, boolean isCpu) {
        String imagePath = prefix + card.toFileName();
        ImageView imageView = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath))));

        imageView.setFitWidth(80);
        imageView.setPreserveRatio(true);

        if (!isCpu) {
            imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleCardClick);
        }
        imageView.setUserData(card);
        return imageView;
    }

    private void handleCardClick(MouseEvent event) {
        // Prevenir múltiples clics o acciones durante el procesamiento
        if (!gameModel.isUserTurn() || gameOver || processingClick) return;

        processingClick = true;

        ImageView clickedCard = (ImageView) event.getSource();
        int cardIndex = userHand.getChildren().indexOf(clickedCard);

        if (cardIndex < 0 || cardIndex >= gameModel.getUserHand().size()) {
            processingClick = false;
            return;
        }

        UnoCard selectedCard = gameModel.getUserHand().get(cardIndex);

        if (gameModel.isValidPlay(selectedCard)) {
            final UnoCard cardToPlay = selectedCard;

            gameView.animateCardPlay(clickedCard, () -> {
                gameModel.playUserCard(cardToPlay);
                userHand.getChildren().remove(clickedCard);
                gameView.updateDiscardPile(gameModel.getTopDiscard());

                // Verificar si el juego ha terminado inmediatamente después de jugar una carta
                if (gameModel.isGameOver()) {
                    gameOver = true;
                    stopAllThreads();
                    gameView.checkGameOver(true, gameModel.userWins());
                    processingClick = false;
                    return;
                }

                // Verificar UNO si quedó una carta
                if (gameModel.getUserHand().size() == 1) {
                    startUserUnoTimer();
                }

                // Manejar cartas especiales
                if (cardToPlay.getValue() == UnoCard.Value.DRAW_TWO) {
                    updateCpuHandAfterDraw();
                    processingClick = false;
                    return;
                }

                if (cardToPlay.getValue() == UnoCard.Value.WILD ||
                        cardToPlay.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {

                    Platform.runLater(() -> {
                        promptColorChoice();

                        if (cardToPlay.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {
                            updateCpuHandAfterDraw();
                            gameView.showAlertSafely("CPU pierde turno",
                                    "La CPU ha robado 4 cartas y pierde su turno.",
                                    Alert.AlertType.INFORMATION);
                            processingClick = false;
                        } else {
                            // Solo para WILD (no +4), programar el turno de la CPU
                            if (!gameOver) {
                                PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
                                delay.setOnFinished(e -> {
                                    if (!gameOver) playCpuTurn();
                                });
                                delay.play();
                            }
                            processingClick = false;
                        }
                    });
                    return;
                }

                if (cardToPlay.getValue() == UnoCard.Value.SKIP ||
                        cardToPlay.getValue() == UnoCard.Value.REVERSE) {
                    processingClick = false;
                    return;
                }

                // Para cartas normales, programar el turno de la CPU
                if (!gameOver) {
                    PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
                    delay.setOnFinished(e -> {
                        if (!gameOver) playCpuTurn();
                    });
                    delay.play();
                }
                processingClick = false;
            });
        } else {
            gameView.shakeAnimation(clickedCard);
            processingClick = false;
        }
    }

    private void updateCpuHandAfterDraw() {
        if (gameOver) return;

        // Actualizar visualmente la mano de la CPU después de que robe cartas
        int previousSize = cpuHand.getChildren().size();
        int currentSize = gameModel.getCpuHand().size();
        int difference = currentSize - previousSize;

        // Agregar nuevas cartas visuales si la CPU ha robado
        for (int i = previousSize; i < currentSize; i++) {
            ImageView newCard = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(prefix + "card_uno.png"))));
            newCard.setFitWidth(80);
            newCard.setPreserveRatio(true);
            cpuHand.getChildren().add(newCard);

            int finalI = i;
            gameView.animateFadeIn(newCard, Duration.millis(300), () -> {
                if (finalI == currentSize - 1) {
                    gameView.showDrawCardsMessage("La CPU ha robado " + difference + " cartas.", true);
                }
            });
        }
    }

    private void promptColorChoice() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Elige un color");
        alert.setHeaderText("Selecciona el nuevo color:");

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

    private void playCpuTurn() {
        if (gameOver) return;

        PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
        delay.setOnFinished(e -> {
            if (gameOver) return;

            UnoCard playedCard = gameModel.playCpuCard();
            if (playedCard != null) {
                // La CPU juega una carta
                if (cpuHand.getChildren().isEmpty()) return;

                ImageView cpuCard = (ImageView) cpuHand.getChildren().get(0);
                gameView.animateCardPlay(cpuCard, () -> {
                    cpuHand.getChildren().remove(0);
                    gameView.updateDiscardPile(gameModel.getTopDiscard());

                    // Verificar si la CPU ganó
                    if (gameModel.isGameOver()) {
                        gameOver = true;
                        stopAllThreads();
                        gameView.checkGameOver(true, gameModel.userWins());
                        return;
                    }

                    // Verificar UNO para CPU
                    if (gameModel.getCpuHand().size() == 1 && !gameOver){
                        startCpuUnoTimer();
                    }

                    // Si la carta era un comodín, mostrar el color elegido
                    if ((playedCard.getValue() == UnoCard.Value.WILD ||
                            playedCard.getValue() == UnoCard.Value.WILD_DRAW_FOUR) &&
                            gameModel.getCurrentColor() != null) {

                        gameView.showCpuColorChoice(gameModel.getCurrentColor());

                        // Si se jugó +4, actualizar visualmente la mano del usuario
                        if (playedCard.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {
                            updateUserHandAfterDraw(4);
                            if (!gameOver) {
                                PauseTransition cpuDelay = new PauseTransition(Duration.seconds(0.5));
                                cpuDelay.setOnFinished(evt -> {
                                    if (!gameOver) playCpuTurn();
                                });
                                cpuDelay.play();
                            }
                        }
                    }
                    // Si jugó +2, actualizar mano del usuario
                    else if (playedCard.getValue() == UnoCard.Value.DRAW_TWO) {
                        updateUserHandAfterDraw(2);
                        if (!gameOver) {
                            PauseTransition cpuDelay = new PauseTransition(Duration.seconds(0.5));
                            cpuDelay.setOnFinished(evt -> {
                                if (!gameOver) playCpuTurn();
                            });
                            cpuDelay.play();
                        }
                    }
                    else if (playedCard.getValue() == UnoCard.Value.REVERSE ||
                            playedCard.getValue() == UnoCard.Value.SKIP) {
                        if (!gameOver) {
                            PauseTransition cpuDelay = new PauseTransition(Duration.seconds(0.5));
                            cpuDelay.setOnFinished(evt -> {
                                if (!gameOver) playCpuTurn();
                            });
                            cpuDelay.play();
                        }
                    }
                });
            } else {
                // La CPU roba una carta
                UnoCard drawnCard = gameModel.drawCard(false);

                // Crear una nueva carta boca abajo para la CPU
                ImageView newCard = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(prefix + "card_uno.png"))));
                newCard.setFitWidth(80);
                newCard.setPreserveRatio(true);

                // Animación para la carta robada
                cpuHand.getChildren().add(newCard);
                gameView.animateFadeIn(newCard, Duration.millis(500), () -> {
                    // Después de robar, cambiar al turno del usuario
                    gameModel.switchTurn();
                });
            }
        });
        delay.play();
    }

    private void updateUserHandAfterDraw(int count) {
        if (gameOver) return;

        // Mostrar visualmente cuando el usuario debe robar después de un +2 o +4
        for (int i = 0; i < count; i++) {
            UnoCard drawnCard = gameModel.getUserHand().get(gameModel.getUserHand().size() - count + i);
            ImageView cardView = createCardImage(drawnCard, false);
            userHand.getChildren().add(cardView);

            gameView.animateFadeIn(cardView, Duration.millis(300), null);
        }
    }

    private void setupUnoButton() {
        unoButton.setVisible(false);
        unoButton.setOnAction(e -> {
            userUnoClicked = true;
            unoButton.setVisible(false);
            // Animación o efecto al presionar UNO
            gameView.showAlertSafely("¡UNO!", "¡Has dicho UNO a tiempo!", Alert.AlertType.INFORMATION);

            if (userUnoThread != null) {
                userUnoThread.interrupt();
                userUnoThread = null;
            }
        });
    }

    private void startUserUnoTimer() {
        if (gameOver) return;

        // Reiniciamos la bandera de UNO
        userUnoClicked = false;

        // Mostramos el botón UNO solo si el usuario tiene una carta
        if (gameModel.getUserHand().size() == 1) {
            // Detener cualquier hilo anterior
            if (userUnoThread != null) {
                userUnoThread.interrupt();
                userUnoThread = null;
            }

            Platform.runLater(() -> unoButton.setVisible(true));

            userUnoThread = new Thread(() -> {
                try {
                    // Temporizador fijo de 3 segundos para que el usuario pulse UNO
                    Thread.sleep(3000);

                    Platform.runLater(() -> {
                        // Si el botón aún está visible y no se ha presionado
                        if (!gameOver && unoButton.isVisible() && !userUnoClicked) {
                            // Aplicar penalización: el usuario roba 2 cartas
                            for (int i = 0; i < 2; i++) {
                                UnoCard penaltyCard = gameModel.drawCard(true);
                                ImageView cardView = createCardImage(penaltyCard, false);
                                userHand.getChildren().add(cardView);
                            }
                            unoButton.setVisible(false);
                            gameView.showDrawCardsMessage(
                                    "Has recibido 2 cartas de penalización por no decir UNO.",
                                    false
                            );
                        } else {
                            // Asegurarse de que el botón esté oculto en todos los casos
                            unoButton.setVisible(false);
                        }
                    });
                } catch (InterruptedException ignored) {
                    // Asegurarse de ocultar el botón si el hilo es interrumpido
                    Platform.runLater(() -> unoButton.setVisible(false));
                }
            }, "HumanUnoTimer");
            userUnoThread.setDaemon(true);
            userUnoThread.start();
        }
    }

    private void startCpuUnoTimer() {
        if (gameOver) return;

        // Reiniciamos las banderas
        cpuDeclaredUno = false;
        playerCaughtCpu = false;

        if (cpuUnoThread != null) {
            cpuUnoThread.interrupt();
            cpuUnoThread = null;
        }

        // Mostrar el botón para atrapar a la CPU si la CPU tiene una carta
        if (gameModel.getCpuHand().size() == 1) {
            Platform.runLater(() -> catchCpuButton.setVisible(true));

            cpuUnoThread = new Thread(() -> {
                try {
                    // Tiempo aleatorio antes de que la CPU declare UNO (entre 1-3 segundos)
                    int delay = 1000 + new Random().nextInt(2000);
                    Thread.sleep(delay);

                    if (gameOver) return;

                    // Verificar si el jugador ha atrapado a la CPU durante este tiempo
                    if (!playerCaughtCpu && gameModel.getCpuHand().size() == 1) {
                        cpuDeclaredUno = true;

                        Platform.runLater(() -> {
                            if (!gameOver) {
                                gameView.showAlertSafely("¡UNO!", "La CPU ha declarado UNO", Alert.AlertType.INFORMATION);

                                // Ocultar el botón de atrapar a la CPU
                                catchCpuButton.setVisible(false);
                            }
                        });
                    }
                } catch (InterruptedException ignored) {
                    Platform.runLater(() -> catchCpuButton.setVisible(false));
                } finally {

                    Platform.runLater(() -> catchCpuButton.setVisible(false));
                }
            }, "CpuUnoTimer");

            cpuUnoThread.setDaemon(true);
            cpuUnoThread.start();
        }
    }

    private void setupCatchCpuButton() {
        catchCpuButton.setVisible(false);
        catchCpuButton.setOnAction(e -> {
            if (gameOver) return;

            playerCaughtCpu = true;
            catchCpuButton.setVisible(false);

            // Detener el hilo de CPU UNO
            if (cpuUnoThread != null) {
                cpuUnoThread.interrupt();
                cpuUnoThread = null;
            }

            // Si el jugador atrapa a la CPU antes de que declare UNO
            if (!cpuDeclaredUno && gameModel.getCpuHand().size() == 1) {
                // Penalizar a la CPU (debe robar 2 cartas)
                gameModel.drawCardsForOpponent(2);
                updateCpuHandAfterDraw();
                gameView.showAlertSafely("¡Atrapada!", "¡Has atrapado a la CPU antes de que dijera UNO!", Alert.AlertType.INFORMATION);
            } else {
                // Si el jugador presiona el botón pero la CPU no tiene una carta o ya declaró UNO
                gameView.showAlertSafely("¡Falsa alarma!", "La CPU no tiene una carta pendiente o ya declaró UNO", Alert.AlertType.WARNING);
            }
        });
    }
}