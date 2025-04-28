module com.example.unogame {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.unogame to javafx.fxml;
    exports com.example.unogame;
    exports com.example.unogame.controller;
    opens com.example.unogame.controller to javafx.fxml;
}