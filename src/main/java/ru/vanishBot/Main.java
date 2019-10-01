package ru.vanishBot;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;


public class Main extends Application{
    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(Main.class.getClassLoader().getResource("view.fxml"));
        AnchorPane root = loader.load();
        Controller controller = loader.getController();
        controller.setOnCloseOperation(primaryStage);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

    }

}

