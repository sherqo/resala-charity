package com.resala.gui.components;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

public final class Card {

    private Card() {}

    public static VBox of(String title, Node content) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new javafx.geometry.Insets(16));

        if (title != null && !title.isBlank()) {
            javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(title);
            titleLabel.getStyleClass().add("card-title");
            card.getChildren().add(titleLabel);
        }

        card.getChildren().add(content);
        return card;
    }

    public static VBox of(Node content) {
        return of(null, content);
    }
}
