package org.volt4.shotstacker;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;

public class ImageEntry extends AnchorPane {

    @FXML
    private ImageView image;

    @FXML
    private Label path;

    private final String pathToImage;

    private double position = 0.5;

    private String status;

    private Consumer<ImageEntry> onAction;

    public ImageEntry(File imgFile, Consumer<ImageEntry> onAction) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ImageEntry.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            image.setImage(new Image(new FileInputStream(imgFile.getAbsolutePath())));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.onAction = onAction;
        pathToImage = imgFile.getAbsolutePath();
        path.setText(imgFile.getAbsolutePath());
        getStyleClass().add("");
        getStyleClass().add("");
    }

    @FXML
    void onClicked(MouseEvent event) {
        onAction.accept(this);
        event.consume();
    }

    public String getStatus() {
        return status;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public void setStatus(String status) {
        this.status = status;
        switch(status) {
            case "s" -> getStyleClass().set(1, "ie-selected");
            case "a" -> getStyleClass().set(1, "ie-a");
            case "b" -> getStyleClass().set(1, "ie-b");
            case "c" -> getStyleClass().set(1, "ie-c");
            case "" -> getStyleClass().set(1, "");
        }
    }

    public String getPath() {
        return pathToImage;
    }

}
