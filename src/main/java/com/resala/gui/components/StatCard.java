package com.resala.gui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;

/**
 * A statistic metric card for dashboard KPIs.
 * Styled in Notion minimal aesthetic: clean typography, neutral borders, no loud top-borders.
 */
public final class StatCard {

    private StatCard() {}

    /**
     * Creates a neutral stat card with label and value.
     *
     * @param label the metric label (e.g., "Total Donations")
     * @param value the initial value text (e.g., "1,234")
     * @return a VBox containing the stat card; call {@code valueLabel(card)} to access the value Label
     */
    public static VBox of(String label, String value) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("card", "stat-card");
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setMinWidth(160);

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("stat-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("stat-value");
        valueNode.setId("stat-value");

        card.getChildren().addAll(labelNode, valueNode);
        return card;
    }

    /**
     * Backward-compatible overload accepting an accent paint.
     * Kept for interface compatibility, keeping styling clean and restrained.
     */
    public static VBox of(String label, String value, Paint accent) {
        return of(label, value);
    }

    /**
     * Extracts the value Label from a StatCard VBox for live updates.
     *
     * @param statCard the VBox returned by {@link #of(String, String)}
     * @return the value Label (second child)
     */
    public static Label valueLabel(VBox statCard) {
        return (Label) statCard.getChildren().get(1);
    }
}
