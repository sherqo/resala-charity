package com.resala.gui.components;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

/**
 * A toolbar/horizontal action group.
 * Stateless utility — returns a styled HBox.
 */
public final class Toolbar {

    private Toolbar() {}

    /**
     * Creates a toolbar with the given buttons.
     *
     * @param buttons one or more pre-styled buttons
     * @return styled HBox toolbar
     */
    public static HBox of(Button... buttons) {
        HBox toolbar = new HBox(10);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setPadding(new javafx.geometry.Insets(0, 0, 12, 0));
        toolbar.getChildren().addAll(buttons);
        return toolbar;
    }

    /**
     * Creates a primary action button (filled, indigo).
     */
    public static Button primary(String text, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("btn", "btn-primary");
        btn.setOnAction(onAction);
        return btn;
    }

    /**
     * Creates a ghost/secondary action button (outlined).
     */
    public static Button ghost(String text, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("btn", "btn-ghost");
        btn.setOnAction(onAction);
        return btn;
    }

    /**
     * Creates a danger/destructive action button (red).
     */
    public static Button danger(String text, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("btn", "btn-danger");
        btn.setOnAction(onAction);
        return btn;
    }
}