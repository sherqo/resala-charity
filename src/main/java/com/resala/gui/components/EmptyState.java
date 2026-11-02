package com.resala.gui.components;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

/**
 * Empty state illustration with optional call-to-action.
 * Stateless utility — returns a styled VBox.
 */
public final class EmptyState {

    private EmptyState() {}

    /**
     * Creates an empty state view.
     *
     * @param emoji   an emoji/icon character (e.g., "📭", "🎁")
     * @param title   the main message (e.g., "No donations yet")
     * @param hint    a secondary hint (e.g., "Click +Add to create one")
     * @param cta     optional call-to-action button (may be null)
     * @return styled VBox centered in available space
     */
    public static VBox of(String emoji, String title, String hint, Button cta) {
        VBox box = new VBox(12);
        box.getStyleClass().addAll("card", "empty-state");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new javafx.geometry.Insets(32));
        VBox.setVgrow(box, javafx.scene.layout.Priority.ALWAYS);

        Label emojiLabel = new Label(emoji);
        emojiLabel.getStyleClass().add("empty-emoji");
        emojiLabel.setStyle("-fx-font-size: 48px;");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");

        Label hintLabel = new Label(hint);
        hintLabel.getStyleClass().add("empty-hint");

        box.getChildren().addAll(emojiLabel, titleLabel, hintLabel);

        if (cta != null) {
            cta.getStyleClass().addAll("btn", "btn-primary");
            box.getChildren().add(cta);
        }

        return box;
    }
}