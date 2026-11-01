package com.resala.gui.components;

import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Non-blocking toast notifications that slide in from bottom-right.
 */
public class Toast {

    public enum Type { INFO, SUCCESS, WARNING, ERROR }

    /**
     * Shows a toast message.
     *
     * @param owner the owner stage (for positioning)
     * @param msg   the message text
     * @param type  the toast type (affects color/icon)
     */
    public void show(Stage owner, String msg, Type type) {
        // Create toast label
        Label label = new Label(msg);
        label.getStyleClass().addAll("toast", "toast-" + type.name().toLowerCase());
        label.setPadding(new javafx.geometry.Insets(12, 16, 12, 16));
        label.setWrapText(true);
        label.setMaxWidth(320);

        // Container for stacking multiple toasts
        VBox container = new VBox(8);
        container.setAlignment(Pos.BOTTOM_RIGHT);
        container.getChildren().add(label);

        // Transparent overlay stage
        Stage toastStage = new Stage(StageStyle.TRANSPARENT);
        toastStage.initOwner(owner);
        StackPane root = new StackPane(container);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        toastStage.setScene(scene);

        // Position at owner's bottom-right
        toastStage.setX(owner.getX() + owner.getWidth() - 360);
        toastStage.setY(owner.getY() + owner.getHeight() - 100);
        toastStage.show();

        // Slide-in animation
        label.setTranslateY(50);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), label);
        slideIn.setToY(0);
        slideIn.play();

        // Auto-dismiss after 3 seconds
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), label);
        slideOut.setDelay(Duration.seconds(3));
        slideOut.setToY(-50);
        slideOut.setOnFinished(e -> toastStage.close());
        slideOut.play();
    }
}