package com.example.unogame.adapters;

import com.example.unogame.view.GameView;
import javafx.scene.control.Alert;

/**
 * Adapter for safe alert display on JavaFX Application Thread.
 *
 * @authors Jhon Steven Angulo Nieves, Braulio Robledo Delgado
 */
public class AlertAdapter {
    private final GameView gameView;

    public AlertAdapter(GameView gameView) {
        this.gameView = gameView;
    }

    /** Show information alert. */
    public void showInfo(String message) {
        gameView.showAlertSafely("Información", message, Alert.AlertType.INFORMATION);
    }
    /** Show warning alert. */
    public void showWarning(String message) {
        gameView.showAlertSafely("Advertencia", message, Alert.AlertType.WARNING);
    }
    /** Show error alert. */
    public void showError(String message) {
        gameView.showAlertSafely("Error", message, Alert.AlertType.ERROR);
    }
}