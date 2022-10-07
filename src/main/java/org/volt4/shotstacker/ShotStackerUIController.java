package org.volt4.shotstacker;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShotStackerUIController extends AnchorPane {

    private static final String[] colors = {"rect-a", "rect-b", "rect-c", "rect-d", "rect-e", "rect-f"};

    @FXML
    private AnchorPane explorer;

    @FXML
    private HBox importHint;

    @FXML
    private TextField dimensions1;

    @FXML
    private TextField dimensions2;

    @FXML
    private TextField aspectRatio1;

    @FXML
    private TextField aspectRatio2;

    @FXML
    private ComboBox<String> resolution;

    @FXML
    private AnchorPane image;

    @FXML
    private AnchorPane overlay;

    @FXML
    private ImageView preview;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private SplitPane split;

    @FXML
    private Slider slider;

    // Used for drag & drop.
    private final double[] offset;

    private ImageEntry selected = null;

    private final List<ImageEntry> importedImages;

    private ImageEntry[] chosenImages;

    private int rows = 3, cols = 1;

    public ShotStackerUIController() {
        offset = new double[] {0, 0};
        importedImages = new ArrayList<>();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ShotStacker.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        scrollPane.widthProperty().addListener((a, b, c) -> recalculateImages());
        resolution.getItems().addAll("4k", "2k", "1k");
        resolution.getSelectionModel().selectFirst();
        dimensions1.textProperty().addListener((a, b, c) -> {
            try {
                if (!dimensions1.getText().isEmpty() && Integer.parseInt(dimensions1.getText()) != 1) {
                    dimensions2.setText("1");
                }
            } catch(Exception e) {
                return;
            }
            updateCanvas();
        });
        dimensions2.textProperty().addListener((a, b, c) -> {
            try {
                if (!dimensions2.getText().isEmpty() && Integer.parseInt(dimensions2.getText()) != 1) {
                    dimensions1.setText("1");
                }
            } catch(Exception e) {
                return;
            }
            updateCanvas();
        });
        aspectRatio1.textProperty().addListener((a, b, c) -> updateCanvas());
        aspectRatio2.textProperty().addListener((a, b, c) -> updateCanvas());
        split.getDividers().get(0).positionProperty().addListener((a, b, c) -> updateCanvas());
        slider.valueProperty().addListener((a, b, c) -> {
            selected.setPosition(c.doubleValue());
            updateCanvas();
        });
        Platform.runLater(() -> updateCanvas());
    }


    private void updateCanvas() {
        try {
            double ratioX = Double.parseDouble(aspectRatio1.getText());
            double ratioY = Double.parseDouble(aspectRatio2.getText());
            rows = Integer.parseInt(dimensions1.getText());
            cols = Integer.parseInt(dimensions2.getText());
            overlay.getChildren().clear();
            if (chosenImages == null) {
                chosenImages = new ImageEntry[Math.max(rows, cols)];
            } else {
                var newImages = new ImageEntry[Math.max(rows, cols)];
                for (int i = 0; i < newImages.length && i < chosenImages.length; i++) {
                    newImages[i] = chosenImages[i];
                }
                for (int i = newImages.length; i < chosenImages.length; i++) {
                    if (chosenImages[i] != null) {
                        chosenImages[i].getStyleClass().set(2, "");
                    }
                }
                chosenImages = newImages;
            }
            double width = Math.min(image.getHeight(), image.getWidth());
            double height = Math.min(image.getHeight(), image.getWidth());
            if (ratioY > ratioX) height *= ratioX / ratioY;
            else width *= ratioY / ratioX;
            var xOffset = width < image.getWidth() ? (image.getWidth() - width) / 2 : 0;
            var yOffset = height < image.getHeight() ? (image.getHeight() - height) / 2 : 0;
            for (int i = 0; i < Math.max(rows, cols); i++) {
                var nPane = new AnchorPane();
                nPane.getStyleClass().add(colors[i % colors.length]);
                nPane.setPrefWidth(width / cols);
                nPane.setPrefHeight(height / rows);
                nPane.setLayoutX(xOffset + (cols == 1 ? 0 : i * nPane.getPrefWidth()));
                nPane.setLayoutY(yOffset + (rows == 1 ? 0 : i * nPane.getPrefHeight()));
                int finalI = i;
                nPane.setOnMouseClicked(e -> {
                    if (selected != null) {
                        selected.getStyleClass().set(2, "ie-" + nPane.getStyleClass().get(0).split("-")[1]);
                        nPane.setOpacity(0.001);
                        if (chosenImages[finalI] != null) {
                            chosenImages[finalI].getStyleClass().set(2, "");
                        }
                        for (int j = 0; j < chosenImages.length; j++) {
                            if (chosenImages[j] == selected) {
                                chosenImages[j] = null;
                            }
                        }
                        chosenImages[finalI] = selected;
                        updateCanvas();
                    }
                });
                overlay.getChildren().add(nPane);
                if (chosenImages[i] != null) {
                    nPane.setOpacity(0.001);
                }
            }
            preview.setFitWidth(width);
            preview.setFitHeight(height);
            preview.setImage(convertToFxImage(buildPreview(width, height)));
        } catch (Exception ignored) {
        }
    }

    private BufferedImage buildPreview(double width, double height) {
        var img = new BufferedImage((int) width - 1, (int) height - 1, BufferedImage.TYPE_INT_RGB);
        var g2 = img.getGraphics();
        if (rows > cols) {
            for (int i = 0; i < chosenImages.length; i++) {
                if (chosenImages[i] != null) {
                    try {
                        Image curImg = ImageIO.read(new File(chosenImages[i].getPath()));
                        double oWidth = curImg.getWidth(null);
                        double oHeight = curImg.getHeight(null);
                        double tWidth = width;
                        double tHeight = height / rows;
                        if (oWidth / oHeight > tWidth / tHeight) {
                            double scalar = (tWidth / tHeight);
                            int dx1 = 0;
                            int dy1 = (int) (height / rows) * i;
                            int dx2 = (int) width;
                            int dy2 = (int) (height / rows) * (i + 1);
                            int sx1 = (int) (chosenImages[i].getPosition() * (oWidth - (oHeight * scalar)));
                            int sy1 = 0;
                            int sx2 = (int) (sx1 + oHeight * scalar);
                            int sy2 = (int) oHeight;
                            g2.drawImage(curImg, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
                        } else {
                            double scalar = (tHeight / tWidth);
                            int dx1 = 0;
                            int dy1 = (int) (height / rows) * i;
                            int dx2 = (int) width;
                            int dy2 = (int) (height / rows) * (i + 1);
                            int sx1 = 0;
                            int sy1 = (int) (chosenImages[i].getPosition() * (oHeight - (oWidth * scalar)));
                            int sx2 = (int) oWidth;
                            int sy2 = (int) (sy1 + oWidth * scalar);
                            g2.drawImage(curImg, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            for (int i = 0; i < chosenImages.length; i++) {
                if (chosenImages[i] != null) {
                    try {
                        Image curImg = ImageIO.read(new File(chosenImages[i].getPath()));
                        double oWidth = curImg.getWidth(null);
                        double oHeight = curImg.getHeight(null);
                        double tWidth = width / cols;
                        double tHeight = height;
                        if (oWidth / oHeight > tWidth / tHeight) {
                            double scalar = (tWidth / tHeight);
                            int dx1 = (int) (width / cols) * i;
                            int dy1 = 0;
                            int dx2 = (int) (width / cols) * (i + 1);
                            int dy2 = (int) (height);
                            int sx1 = (int) (chosenImages[i].getPosition() * (oWidth - (oHeight * scalar)));
                            int sy1 = 0;
                            int sx2 = (int) (sx1 + oHeight * scalar);
                            int sy2 = (int) oHeight;
                            g2.drawImage(curImg, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
                        } else {
                            double scalar = (tHeight / tWidth);
                            int dx1 = (int) (width / cols) * i;
                            int dy1 = 0;
                            int dx2 = (int) (width / cols) * (i + 1);
                            int dy2 = (int) (height);
                            int sx1 = 0;
                            int sy1 = (int) (chosenImages[i].getPosition() * (oHeight - (oWidth * scalar)));
                            int sx2 = (int) oWidth;
                            int sy2 = (int) (sy1 + oWidth * scalar);
                            g2.drawImage(curImg, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return img;
    }

    private static javafx.scene.image.Image convertToFxImage(BufferedImage image) {
        WritableImage wr = null;
        if (image != null) {
            wr = new WritableImage(image.getWidth(), image.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    pw.setArgb(x, y, image.getRGB(x, y));
                }
            }
        }
        return new ImageView(wr).getImage();
    }

    @FXML
    void onBackClicked(MouseEvent event) {
        importedImages.forEach(i -> i.setStatus(""));
        selected = null;
    }

    @FXML
    void onReset(MouseEvent event) {
        importedImages.forEach(i -> i.getStyleClass().set(2, ""));
        importedImages.forEach(i -> i.setPosition(0.5));
        chosenImages = null;
        updateCanvas();
    }

    private void recalculateImages() {
        importHint.setVisible(importedImages.size() <= 0);
        var width = scrollPane.getWidth(); // Get explorer width.
        width -= 33.33; // Padding on one side.
        width /= 183.33; // Divide by size of entry.
        int widthFit = (int) width; // Floor is the fit.
        for (int i = 0; i < importedImages.size(); i++) {
            var image = importedImages.get(i);
            int yLevel = i / widthFit;
            int xLevel = i % widthFit;
            image.setLayoutX(33.33 + xLevel * 183.33);
            image.setLayoutY(33.33 + yLevel * 183.33);
        }
        explorer.setPrefHeight(33.33 + (double) (importedImages.size() / widthFit) * 183.33);
        explorer.setPrefWidth(33.33 + widthFit * 183.33);
    }

    private void onSelect(ImageEntry e) {
        String currentSelecting = "";
        switch(currentSelecting) {
            case "" -> {
                importedImages.forEach(i -> i.setStatus(""));
                e.setStatus("s");
            }
        }
        selected = e;
        slider.setValue(selected.getPosition());
    }

    /**
     * Imports images.
     * @param images Images to import.
     */
    public void importImages(List<File> images) {
        var imported = images.stream().map(f -> new ImageEntry(f, this::onSelect)).toList();
        importedImages.addAll(imported);
        explorer.getChildren().addAll(imported);
        recalculateImages();
    }

    @FXML
    void onClose(MouseEvent event) {
        Platform.exit();
    }

    @FXML
    void onMinimize(MouseEvent event) {
        ShotStackerApplication.primaryStage.setIconified(true);
    }

    @FXML
    void onSaveImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpeg", "*.jpg"));
        fileChooser.setTitle("Save Image");
        var path = fileChooser.showSaveDialog(ShotStackerApplication.primaryStage);
        if (path == null)
            return;
        double ratio = Double.parseDouble(aspectRatio1.getText()) / Double.parseDouble(aspectRatio2.getText());
        double width;
        switch(resolution.getSelectionModel().getSelectedItem()) {
            case "4k" -> width = 3840;
            case "2k" -> width = 1920;
            default -> width = 1024;
        }
        double height = width * ratio;
        var img = buildPreview(width, height);
        try {
            ImageIO.write(img, "png", path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onDragBarDragged(MouseEvent event) {
        ShotStackerApplication.primaryStage.setX(event.getScreenX() - offset[0]);
        ShotStackerApplication.primaryStage.setY(event.getScreenY() - offset[1]);
    }

    @FXML
    void onDragBarPressed(MouseEvent event) {
        offset[0] = event.getSceneX();
        offset[1] = event.getSceneY();
    }

    @FXML void onMinimizeDragged(MouseEvent event) { event.consume(); }
    @FXML void onMinimizePressed(MouseEvent event) { event.consume(); }
    @FXML void onCloseDragged(MouseEvent event) { event.consume(); }
    @FXML void onClosePressed(MouseEvent event) { event.consume(); }

}
