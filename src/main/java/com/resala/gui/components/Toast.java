package com.resala.gui.components;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Non-blocking toast notifications that slide in quietly from bottom-right.
 * Styled in Notion minimal aesthetic: crisp dark pill, muted typography, gentle transitions.
 */
public class Toast {

    public enum Type { INFO, SUCCESS, WARNING, ERROR }

    /**
     * Shows a toast message.
     *
     * @param owner the owner stage (for positioning)
     * @param msg   the message text
     * @param type  the toast type
     */
    public void show(Stage owner, String msg, Type type) {
        if (owner == null) return;

        Label label = new Label(msg);
        label.getStyleClass().addAll("toast", "toast-" + type.name().toLowerCase());
        label.setPadding(new Insets(10, 14, 10, 14));
        label.setWrapText(true);
        label.setMaxWidth(340);

        VBox container = new VBox(8);
        container.setAlignment(Pos.BOTTOM_RIGHT);
        container.getChildren().add(label);

        Stage toastStage = new Stage(StageStyle.TRANSPARENT);
        toastStage.initOwner(owner);

        StackPane root = new StackPane(container);
        root.setStyle("-fx-background-color: transparent;");

        if (owner != null && owner.getScene() != null && owner.getScene().getRoot() != null
                && owner.getScene().getRoot().getStyleClass().contains("dark-theme")) {
            root.getStyleClass().add("dark-theme");
        }

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        if (getClass().getResource("/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        }
        toastStage.setScene(scene);

        // Position at owner's bottom-right
        toastStage.setX(owner.getX() + owner.getWidth() - 380);
        toastStage.setY(owner.getY() + owner.getHeight() - 80);
        toastStage.show();

        // Subtle slide & fade in
        label.setOpacity(0);
        label.setTranslateY(10);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), label);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(150), label);
        slideIn.setToY(0);

        fadeIn.play();
        slideIn.play();

        // Auto-dismiss after 2.8 seconds
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), label);
        fadeOut.setDelay(Duration.millis(2800));
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> toastStage.close());
        fadeOut.play();
    }
}
