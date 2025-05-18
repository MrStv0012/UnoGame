package com.example.unogame.view.interfaces;

import com.example.unogame.model.UnoCard;
import javafx.scene.control.Alert;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/**
 * Interface defining view operations for the UNO Game UI.
 *
 * @authors
 *   Jhon Steven Angulo Nieves
 *   Braulio Robledo Delgado
 */
public interface IGameView {
    void updateDiscardPile(UnoCard card);
    void showAlertSafely(String title, String message, Alert.AlertType type);
    void animateCardPlay(ImageView card, Runnable onFinished);
}

