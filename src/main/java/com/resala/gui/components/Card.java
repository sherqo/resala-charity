package com.resala.gui.components;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * A simple card container with optional title.
 * Stateless utility — returns a styled VBox.
 */
public final class Card {

    private Card() {}

    /**
     * Creates a card with a title and content.
     *
     * @param title   the card title (may be null/empty for no title)
     * @param content the content node
     * @return styled VBox card
     */
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

    /**
     * Creates a card with only content (no title).
     *
     * @param content the content node
     * @return styled VBox card
     */
    public static VBox of(Node content) {
        return of(null, content);
    }
}