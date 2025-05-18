package com.example.unogame;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

/**
 * Main JavaFX application class for the UNO Game.
 *
 * @authors
 *   Jhon Steven Angulo Nieves
 *   Braulio Robledo Delgado
 * @version 1.0
 */

public class UnoApplication extends Application {

    /**
     * Initializes and shows the primary stage for the application.
     *
     * @param stage the primary stage provided by the JavaFX runtime.
     * @throws Exception if the FXML file cannot be loaded.
     */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/unogame/game-view.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 600);

        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/com/example/unogame/styles.css"))

                        .toExternalForm()
        );

        stage.setTitle("UNO GAME");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Main entry point of the application.
     *
     * @param args the command-line arguments.
     */
    public static void main(String[] args) {
        launch();
    }

}

