package com.resala.gui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Empty state placeholder with clean typography and optional call-to-action.
 * Follows Notion minimalist restraint: no emojis, no gratuitous card containers.
 */
public final class EmptyState {

    private EmptyState() {}

    /**
     * Creates a clean empty state view.
     *
     * @param title the main message (e.g., "No donations yet")
     * @param hint  a secondary hint (e.g., "Create a donation to get started")
     * @param cta   optional call-to-action button (may be null)
     * @return styled VBox centered in available space
     */
    public static VBox of(String title, String hint, Button cta) {
        VBox box = new VBox(6);
        box.getStyleClass().add("empty-state");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32));
        VBox.setVgrow(box, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");

        box.getChildren().add(titleLabel);

        if (hint != null && !hint.isBlank()) {
            Label hintLabel = new Label(hint);
            hintLabel.getStyleClass().add("empty-hint");
            box.getChildren().add(hintLabel);
        }

        if (cta != null) {
            cta.getStyleClass().addAll("btn", "btn-primary");
            VBox.setMargin(cta, new Insets(10, 0, 0, 0));
            box.getChildren().add(cta);
        }

        return box;
    }

    /**
     * Creates a clean empty state view without CTA.
     */
    public static VBox of(String title, String hint) {
        return of(title, hint, null);
    }

    /**
     * Backward-compatible overload accepting a legacy icon/emoji parameter.
     * The emoji is intentionally ignored to adhere to the core principle: no emojis as visual elements.
     */
    public static VBox of(String unusedIcon, String title, String hint, Button cta) {
        return of(title, hint, cta);
    }
}
