package com.resala.gui.components;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public final class Toolbar {

    private Toolbar() {}

    public static HBox of(Button... buttons) {
        HBox toolbar = new HBox(10);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setPadding(new javafx.geometry.Insets(0, 0, 12, 0));
        toolbar.getChildren().addAll(buttons);
        return toolbar;
    }

    public static Button primary(String text, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("btn", "btn-primary");
        btn.setOnAction(onAction);
        return btn;
    }

    public static Button ghost(String text, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("btn", "btn-ghost");
        btn.setOnAction(onAction);
        return btn;
    }

    public static Button danger(String text, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("btn", "btn-danger");
        btn.setOnAction(onAction);
        return btn;
    }
}