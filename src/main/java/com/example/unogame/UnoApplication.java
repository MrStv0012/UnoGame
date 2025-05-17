package com.example.unogame;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


import java.util.Objects;

public class UnoApplication extends Application {

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

    public static void main(String[] args) {
        launch();
    }

}

