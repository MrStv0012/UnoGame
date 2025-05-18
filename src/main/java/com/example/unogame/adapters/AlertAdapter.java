package com.example.unogame.adapters;

import com.example.unogame.view.GameView;
import javafx.scene.control.Alert;

public class AlertAdapter {
    private final GameView gameView;

    public AlertAdapter(GameView gameView) {
        this.gameView = gameView;
    }

    public void showInfo(String message) {
        gameView.showAlertSafely("Información", message, Alert.AlertType.INFORMATION);
    }

    public void showWarning(String message) {
        gameView.showAlertSafely("Advertencia", message, Alert.AlertType.WARNING);
    }

    public void showError(String message) {
        gameView.showAlertSafely("Error", message, Alert.AlertType.ERROR);
    }
}