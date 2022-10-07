package org.volt4.shotstacker;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ShotStackerApplication extends Application {

    public static Stage primaryStage;

    public static ShotStackerUIController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        ShotStackerApplication.primaryStage = primaryStage;
        primaryStage.initStyle(StageStyle.UNDECORATED);
        var scene = new Scene(controller = new ShotStackerUIController());

        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.I, KeyCombination.META_DOWN), () -> {
            var fileChooser = new FileChooser();
            fileChooser.setTitle("Open Images");
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"));
            var images = fileChooser.showOpenMultipleDialog(primaryStage);
            controller.importImages(images);
        });

        scene.getStylesheets().add("style.css");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
