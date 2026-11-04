package com.resala.gui.components;

import com.resala.gui.AppShell;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

// Small read-only text popup used by receipts, statements and reports.
public final class PreviewDialog {

    private PreviewDialog() {}

    public static void show(AppShell appShell, String title, String text) {
        if (appShell != null && appShell.getModalOverlay() != null) {
            ModalOverlay.show(appShell, new ModalOverlay.ModalCard<Void>() {
                private Consumer<Void> closeCallback;

                @Override
                public Node getNode() {
                    VBox card = new VBox();
                    card.getStyleClass().add("modal-card");
                    card.setMaxWidth(560);
                    card.setMinWidth(480);
                    card.setMaxHeight(560);

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
                        if (closeCallback != null) closeCallback.accept(null);
                    });
                    header.getChildren().addAll(titleLabel, spacer, closeBtn);

                    VBox body = new VBox(10);
                    body.getStyleClass().add("modal-body");

                    TextArea area = new TextArea(text);
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.getStyleClass().add("preview-area");
                    VBox.setVgrow(area, Priority.ALWAYS);

                    body.getChildren().add(area);

                    HBox footer = new HBox(8);
                    footer.getStyleClass().add("modal-footer");
                    footer.setAlignment(Pos.CENTER_RIGHT);

                    Button doneBtn = new Button("Close");
                    doneBtn.getStyleClass().addAll("btn", "btn-ghost");
                    doneBtn.setOnAction(e -> {
                        if (closeCallback != null) closeCallback.accept(null);
                    });
                    footer.getChildren().add(doneBtn);

                    card.getChildren().addAll(header, body, footer);
                    return card;
                }

                @Override
                public void setCloseHandler(Consumer<Void> closeCallback) {
                    this.closeCallback = closeCallback;
                }
            });
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        if (appShell != null && appShell.getStage() != null) {
            dialog.initOwner(appShell.getStage());
        }
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(16);
        dialog.getDialogPane().setContent(area);
        dialog.showAndWait();
    }
}
