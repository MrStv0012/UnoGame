package com.example.unogame.controller;

import com.example.unogame.exceptions.DeckEmptyException;
import com.example.unogame.exceptions.InvalidCardPlayException;
import com.example.unogame.model.GameModel;
import com.example.unogame.model.UnoCard;
import com.example.unogame.model.UnoDeck;
import com.example.unogame.view.GameView;
import javafx.event.EventHandler;

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

import java.util.*;

public class GameViewController {
    @FXML private HBox cpuHand;
    @FXML private HBox userHand;
    @FXML private StackPane centerPane;
    @FXML private ImageView deckView;
    @FXML private ImageView discardPileView;
    @FXML private Button unoButton;
    @FXML private Button catchCpuButton;

    private GameModel gameModel;
    private UnoDeck deck;
    private GameView gameView;
    private final String prefix = "/com/example/unogame/cards-uno/";
    private volatile boolean gameOver = false;
    private volatile boolean userUnoClicked = false;
    private Thread userUnoThread, cpuUnoThread;
    private volatile boolean cpuDeclaredUno = false;
    private volatile boolean playerCaughtCpu = false;
    private boolean processingClick = false; // Para evitar múltiples clics rápidos

    @FXML
    private void initialize() throws DeckEmptyException {
        resetGame();
    }

    private void resetGame() throws DeckEmptyException {
        // Ejecutar en el hilo de UI para mayor seguridad
        Platform.runLater(() -> {
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

            try {
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
            } catch (DeckEmptyException e) {
                gameView.showAlertSafely("Error", "No se pudo inicializar el juego: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
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

    private void handleInitialCard() {
        UnoCard topCard = gameModel.getTopDiscard();

        // Lo primero es actualizar la interfaz gráfica para mostrar la carta inicial
        gameView.updateDiscardPile(topCard);

        // Usar Platform.runLater para asegurar que esto ocurra después de que la interfaz se haya dibujado
        Platform.runLater(() -> {
            try {
                // Si la primera carta es un comodín
                if (topCard.getValue() == UnoCard.Value.WILD ||
                        topCard.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {

                    // Solicitar elección de color
                    promptColorChoice();

                    // Si es +4, aplicar efecto (el jugador roba 4)
                    if (topCard.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {
                        for (int i = 0; i < 4; i++) {
                            UnoCard drawnCard = gameModel.drawCard();
                            ImageView cardView = createCardImage(drawnCard, false);
                            userHand.getChildren().add(cardView);
                        }
                        gameView.showAlertSafely("Carta inicial +4",
                                "Has robado 4 cartas por la carta inicial.",
                                Alert.AlertType.INFORMATION);
                    }

                    // Como es comodín inicial, se confirma que la CPU empieza el turno
                    gameModel.setUserTurn(true);
                }
                // Si es otra carta especial +2
                else if (topCard.getValue() == UnoCard.Value.DRAW_TWO) {
                    // El jugador roba 2 cartas
                    for (int i = 0; i < 2; i++) {
                        UnoCard drawnCard = gameModel.drawCard();
                        ImageView cardView = createCardImage(drawnCard, false);
                        userHand.getChildren().add(cardView);
                    }
                    gameView.showAlertSafely("Carta inicial +2",
                            "Has robado 2 cartas por la carta inicial.",
                            Alert.AlertType.INFORMATION);

                    // La CPU empieza su turno
                    gameModel.setUserTurn(true);
                }
                // Si es SKIP o REVERSE
                else if (topCard.getValue() == UnoCard.Value.SKIP ||
                        topCard.getValue() == UnoCard.Value.REVERSE) {
                    gameView.showAlertSafely("Turno inicial",
                            "La carta inicial es un " + topCard.getValue() + ". Comienza la CPU.",
                            Alert.AlertType.INFORMATION);

                    // La CPU empieza su turno
                    gameModel.setUserTurn(false);

                    // Programar turno de la CPU con delay
                    PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                    pause.setOnFinished(e -> {
                        if (!gameOver) playCpuTurn();
                    });
                    pause.play();
                }
            } catch (DeckEmptyException e) {
                gameView.showAlertSafely("Error", "Error al manejar carta inicial: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void applyInitialDraw(int count) throws DeckEmptyException {
        // Si la carta inicial es un +2 o +4, el jugador roba cartas
        for (int i = 0; i < count; i++) {
            UnoCard drawnCard = gameModel.drawCard();
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
            imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> handleCardClick(event));
        }
        imageView.setUserData(card);
        return imageView;
    }

    private void handleCardClick(MouseEvent event) {
        new CardClickHandler().handle(event);
    }

    // Implementar clase interna CardClickHandler
    private class CardClickHandler implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            // Prevenir múltiples clics o acciones durante el procesamiento
            if (!gameModel.isUserTurn() || gameOver || processingClick) return;

            processingClick = true;

            ImageView clickedCard = (ImageView) event.getSource();
            UnoCard selectedCard = (UnoCard) clickedCard.getUserData();

            if (selectedCard == null) {
                gameView.showAlertSafely("Error", "No se pudo obtener información de la carta", Alert.AlertType.ERROR);
                processingClick = false;
                return;
            }

            // Buscar la carta equivalente en la mano del usuario
            UnoCard cardToPlay = null;
            for (UnoCard card : gameModel.getUserHand()) {
                if (card.getColor() == selectedCard.getColor() &&
                        card.getValue() == selectedCard.getValue()) {
                    cardToPlay = card;
                    break;
                }
            }

            if (cardToPlay != null) {
                if (gameModel.isValidPlay(cardToPlay)) {
                    final UnoCard finalCardToPlay = cardToPlay;

                    gameView.animateCardPlay(clickedCard, () -> {
                        try {
                            gameModel.playCard(finalCardToPlay, true);
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
                                Platform.runLater(() -> {
                                    unoButton.setVisible(true);
                                    unoButton.toFront();
                                });
                                startUserUnoTimer();
                            }

                            // Manejar cartas especiales
                            handleSpecialCardEffects(finalCardToPlay);

                        } catch (InvalidCardPlayException e) {
                            gameView.showAlertSafely("Error", "Carta no valida para jugar", Alert.AlertType.ERROR);
                            processingClick = false;
                        } catch (DeckEmptyException e) {
                            gameView.showAlertSafely("Sin cartas", "No quedan cartas en el mazo: " + e.getMessage(), Alert.AlertType.WARNING);
                            processingClick = false;
                        } catch (Exception e) {
                            gameView.showAlertSafely("Error", "Error inesperado: " + e.getMessage(), Alert.AlertType.ERROR);
                            processingClick = false;
                        }
                    });
                } else {
                    gameView.shakeAnimation(clickedCard);
                    processingClick = false;
                }
            }
        }
    }

    private void handleDrawCard() {
        if (gameOver || !gameModel.isUserTurn()) return;

        processingClick = true;

        try {
            // En el caso donde el usuario PUEDE jugar una carta pero decide robar
            // debemos permitirle robar igualmente y no mostrar error

            // Animar la carta robada
            UnoCard drawnCard = gameModel.drawUserCard();
            ImageView cardView = createCardImage(drawnCard, false);

            // Añadir la carta a la mano con animación
            userHand.getChildren().add(cardView);
            gameView.animateFadeIn(cardView, Duration.millis(500), () -> {
                // Verificar si la carta recién robada se puede jugar
                if (gameModel.isValidPlay(drawnCard)) {
                    // La carta se puede jugar, pero el jugador decide si lo hace
                    processingClick = false;
                } else {
                    // La carta no se puede jugar, pasar automáticamente al turno de la CPU
                    processingClick = false;
                    gameModel.switchTurn();

                    if (!gameOver) {
                        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
                        pause.setOnFinished(pe -> {
                            if (!gameOver) playCpuTurn();
                        });
                        pause.play();
                    }
                }
            });
        } catch (DeckEmptyException e) {
            gameView.showAlertSafely("Error", e.getMessage(), Alert.AlertType.ERROR);
            processingClick = false;
        }
    }


    private void handleSpecialCardEffects(UnoCard cardToPlay) {
        try {
            if (cardToPlay.getValue() == UnoCard.Value.DRAW_TWO) {
                drawCardAndUpdateHand(false, 2);
                processingClick = false;
                return;
            }

            if (cardToPlay.getValue() == UnoCard.Value.WILD ||
                    cardToPlay.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {

                Platform.runLater(() -> {
                    try {
                        promptColorChoice();

                        if (cardToPlay.getValue() == UnoCard.Value.WILD_DRAW_FOUR) {
                            drawCardAndUpdateHand(false, 4);
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
                    } catch (Exception e) {
                        gameView.showAlertSafely("Error", "Error al procesar carta especial: " + e.getMessage(), Alert.AlertType.ERROR);
                        processingClick = false;
                    }
                });
                return;
            }

            if (cardToPlay.getValue() == UnoCard.Value.SKIP ||
                    cardToPlay.getValue() == UnoCard.Value.REVERSE) {
                // CORRECCIÓN: Mostrar mensaje informativo y NO cambiar al turno de la CPU
                // ya que el efecto de SKIP/REVERSE hace que el usuario mantenga su turno
                Platform.runLater(() -> {
                    String mensaje = cardToPlay.getValue() == UnoCard.Value.SKIP ?
                            "¡Has saltado el turno de la CPU!" :
                            "¡Has cambiado el sentido del juego!";
                    gameView.showAlertSafely("Turno de CPU saltado", mensaje, Alert.AlertType.INFORMATION);
                });
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
        } catch (Exception e) {
            gameView.showAlertSafely("Error", "Error inesperado: " + e.getMessage(), Alert.AlertType.ERROR);
            processingClick = false;
        }
    }


    /**
     * Método centralizado para manejar el proceso de robar múltiples cartas.
     * @param isUser true si las cartas son para el usuario, false si son para la CPU
     * @param count número de cartas a robar (por defecto 1)
     * @return Lista de vistas de cartas creadas
     * @throws DeckEmptyException si no hay más cartas en el mazo
     */
    private List<ImageView> drawCardAndUpdateHand(boolean isUser, int count)
            throws DeckEmptyException {
        List<ImageView> cardViews = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // 1) Robo y actualizo modelo de una sola vez
            UnoCard card = isUser
                    ? gameModel.drawUserCard()
                    : gameModel.drawCpuCard();

            // 2) Creo la vista adecuada
            ImageView view;
            if (isUser) {
                view = createCardImage(card, false);
                userHand.getChildren().add(view);
            } else {
                // reverso para la CPU
                view = new ImageView(new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream(prefix + "card_uno.png"))
                ));
                // CORRECCIÓN: Asegurar tamaño correcto para cartas de CPU
                view.setFitWidth(80);
                view.setPreserveRatio(true);
                cpuHand.getChildren().add(view);
                gameView.animateFadeIn(view, Duration.millis(500), () -> {});
            }
            // 3) Animo y guardo
            cardViews.add(view);
        }

        return cardViews;
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

        // IMPORTANTE: Evitar problemas de concurrencia asegurando que estamos en el hilo de la UI
        Platform.runLater(() -> {
            PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
            delay.setOnFinished(e -> {
                if (gameOver) return;

                try {
                    UnoCard playedCard = gameModel.playCard(null, false);
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
                                    try {
                                        drawCardAndUpdateHand(true, 4);
                                    } catch (DeckEmptyException ex) {
                                        throw new RuntimeException(ex);
                                    }
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
                                try {
                                    drawCardAndUpdateHand(true,2);
                                } catch (DeckEmptyException ex) {
                                    throw new RuntimeException(ex);
                                }
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
                        try {
                            // CORREGIDO: Verificar que hay cartas disponibles
                            UnoCard drawnCard = gameModel.drawCpuCard();

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
                        } catch (DeckEmptyException ex) {
                            // CORREGIDO: Manejar caso cuando el mazo está vacío
                            gameView.showAlertSafely("Sin cartas", "No quedan cartas en el mazo.", Alert.AlertType.WARNING);
                            gameModel.switchTurn(); // Dar turno al jugador
                        }
                    }
                } catch (Exception ex) {
                    // Manejar excepciones en la lógica de juego
                    gameView.showAlertSafely("Error", "Error en el turno de la CPU: " + ex.getMessage(), Alert.AlertType.ERROR);
                    // En caso de error, dar turno al jugador
                    gameModel.switchTurn();
                }
            });
            delay.play();
        });
    }


    private void setupUnoButton() {
        unoButton.setVisible(false);
        unoButton.setOnAction(new UnoButtonHandler());
    }

    private class UnoButtonHandler implements EventHandler<javafx.event.ActionEvent> {
        @Override
        public void handle(javafx.event.ActionEvent e) {
            userUnoClicked = true;
            unoButton.setVisible(false);
            // Animación o efecto al presionar UNO
            gameView.showAlertSafely("¡UNO!", "¡Has dicho UNO a tiempo!", Alert.AlertType.INFORMATION);

            if (userUnoThread != null) {
                userUnoThread.interrupt();
                userUnoThread = null;
            }
        }
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

            Platform.runLater(() -> {
                unoButton.setVisible(true);
                unoButton.toFront();
                unoButton.setDisable(false);
            });

            userUnoThread = new Thread(new TimerHandler(true), "HumanUnoTimer");
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

            cpuUnoThread = new Thread(new TimerHandler(false), "CpuUnoTimer");
            cpuUnoThread.setDaemon(true);
            cpuUnoThread.start();
        }
    }

    // Reemplaza la clase TimerHandler existente en GameViewController.java
    private class TimerHandler implements Runnable {
        private final boolean isUserTimer;

        public TimerHandler(boolean isUserTimer) {
            this.isUserTimer = isUserTimer;
        }

        @Override
        public void run() {
            try {
                // El tiempo depende de si es usuario o CPU
                if (isUserTimer) {
                    // Temporizador fijo de 3 segundos para que el usuario pulse UNO
                    Thread.sleep(3000);

                    // Verificar si el juego sigue activo antes de hacer cualquier cosa
                    if (gameOver) return;

                    Platform.runLater(() -> {
                        // Si el botón aún está visible y no se ha presionado
                        if (!gameOver && unoButton.isVisible() && !userUnoClicked) {
                            // Aplicar penalización: el usuario SIEMPRE roba 2 cartas
                            try {
                                // Llamar al método centralizado para robar 2 cartas
                                drawCardAndUpdateHand(true, 2);
                                unoButton.setVisible(false);
                                gameView.showDrawCardsMessage(
                                        "Has recibido 2 cartas de penalización por no decir UNO.",
                                        false
                                );
                            } catch (DeckEmptyException e) {
                                gameView.showAlertSafely("Error", e.getMessage(), Alert.AlertType.ERROR);
                            } finally {
                                // Asegurar que el botón esté oculto
                                unoButton.setVisible(false);
                            }
                        } else {
                            // Asegurarse de que el botón esté oculto en todos los casos
                            unoButton.setVisible(false);
                        }
                    });
                } else {
                    // Tiempo aleatorio antes de que la CPU declare UNO (entre 1-3 segundos)
                    int delay = 1000 + new Random().nextInt(2000);
                    Thread.sleep(delay);

                    // Verificar si el juego sigue activo
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
                }
            } catch (InterruptedException ignored) {
                // Simplemente terminar el hilo si es interrumpido
            } finally {
                // Asegurar que los botones se oculten en el hilo de UI
                Platform.runLater(() -> {
                    if (isUserTimer) {
                        unoButton.setVisible(false);
                    } else {
                        catchCpuButton.setVisible(false);
                    }
                });
            }
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
                // Penalizar a la CPU - SIEMPRE debe robar exactamente 2 cartas
                try {
                    // Usar nuestro método centralizado para robar 2 cartas
                    drawCardAndUpdateHand(false, 2);
                    gameView.showAlertSafely("¡Atrapada!", "¡Has atrapado a la CPU antes de que dijera UNO!\nLa CPU ha robado 2 cartas de penalización.", Alert.AlertType.INFORMATION);
                } catch (DeckEmptyException ex) {
                    gameView.showAlertSafely("Error", "No hay suficientes cartas para la penalización: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            } else {
                // Si el jugador presiona el botón pero la CPU no tiene una carta o ya declaró UNO
                gameView.showAlertSafely("¡Falsa alarma!", "La CPU no tiene una carta pendiente o ya declaró UNO", Alert.AlertType.WARNING);
            }
        });
    }
}