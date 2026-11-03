package com.resala.gui.components;

import com.resala.gui.AppShell;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.function.Consumer;

public class ConfirmDialog {

    public boolean ask(Window owner, String message) {
        boolean isDelete = message.toLowerCase().contains("delete") || message.toLowerCase().contains("archive");
        return ask(owner, isDelete ? "Delete Confirmation" : "Confirm Action", message);
    }

    public boolean ask(Window owner, String title, String message) {
        AppShell appShell = null;
        if (owner != null && owner.getScene() != null && owner.getScene().getUserData() instanceof AppShell shell) {
            appShell = shell;
        } else {
            appShell = AppShell.getInstance();
        }

        if (appShell != null && appShell.getModalOverlay() != null) {
            return showInAppModal(appShell, title, message);
        }

        return showFallbackAlert(owner, title, message);
    }

    private boolean showInAppModal(AppShell appShell, String title, String message) {
        Boolean res = ModalOverlay.show(appShell, new ModalOverlay.ModalCard<>() {
            private Consumer<Boolean> closeCallback;

            @Override
            public Node getNode() {
                VBox card = new VBox();
                card.getStyleClass().add("modal-card");
                card.setMaxWidth(420);
                card.setMinWidth(360);

                // Header
                HBox header = new HBox();
                header.getStyleClass().add("modal-header");
                header.setAlignment(Pos.CENTER_LEFT);

                Label titleLabel = new Label(title);
                titleLabel.getStyleClass().add("modal-title");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button closeBtn = new Button("✕");
                closeBtn.getStyleClass().add("modal-close-btn");
                closeBtn.setOnAction(e -> {
                    if (closeCallback != null) closeCallback.accept(false);
                });

                header.getChildren().addAll(titleLabel, spacer, closeBtn);

                // Body
                VBox body = new VBox(10);
                body.getStyleClass().add("modal-body");

                Label msgLabel = new Label(message);
                msgLabel.getStyleClass().add("modal-message");
                msgLabel.setWrapText(true);
                msgLabel.setMaxWidth(380);
                body.getChildren().add(msgLabel);

                // Footer
                HBox footer = new HBox(8);
                footer.getStyleClass().add("modal-footer");
                footer.setAlignment(Pos.CENTER_RIGHT);

                Region footerSpacer = new Region();
                HBox.setHgrow(footerSpacer, Priority.ALWAYS);

                Button cancelBtn = new Button("Cancel");
                cancelBtn.getStyleClass().addAll("btn", "btn-ghost");
                cancelBtn.setOnAction(e -> {
                    if (closeCallback != null) closeCallback.accept(false);
                });

                boolean isDelete = title.toLowerCase().contains("delete") || message.toLowerCase().contains("delete")
                        || title.toLowerCase().contains("archive") || message.toLowerCase().contains("archive");

                Button confirmBtn = new Button(isDelete ? "Delete" : "Confirm");
                confirmBtn.getStyleClass().addAll("btn", isDelete ? "btn-danger" : "btn-primary");
                confirmBtn.setOnAction(e -> {
                    if (closeCallback != null) closeCallback.accept(true);
                });

                footer.getChildren().addAll(footerSpacer, cancelBtn, confirmBtn);
                card.getChildren().addAll(header, body, footer);
                return card;
            }

            @Override
            public void setCloseHandler(Consumer<Boolean> closeCallback) {
                this.closeCallback = closeCallback;
            }
        });

        return Boolean.TRUE.equals(res);
    }

    private boolean showFallbackAlert(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
