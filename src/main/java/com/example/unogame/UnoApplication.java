package com.example.unogame;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/unogame/game-view.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 600);
        // Carga CSS (está junto al FXML en el mismo paquete)
        scene.getStylesheets().add(
                getClass().getResource("/com/example/unogame/styles.css")
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
