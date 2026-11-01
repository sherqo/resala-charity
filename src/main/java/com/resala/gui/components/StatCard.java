package com.resala.gui.components;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * A statistic card for dashboard KPIs.
 * Stateless utility — returns a styled VBox with an exposed value label for live updates.
 */
public final class StatCard {

    private StatCard() {}

    /**
     * Creates a stat card with label, value, and accent color.
     *
     * @param label  the metric label (e.g., "Total Donations")
     * @param value  the initial value text (e.g., "1,234")
     * @param accent the accent color for the top border
     * @return a VBox containing the stat card; call {@code getChildren().get(1)} to access the value Label
     */
    public static VBox of(String label, String value, Paint accent) {
        VBox card = new VBox(4);
        card.getStyleClass().addAll("card", "stat-card");
        card.setPadding(new javafx.geometry.Insets(16));
        card.setMinWidth(180);

        // Add accent color style class based on the paint
        String accentClass = getAccentClass(accent);
        if (accentClass != null) {
            card.getStyleClass().add(accentClass);
        }

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("stat-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("stat-value");
        valueNode.setId("stat-value"); // for potential lookup

        card.getChildren().addAll(labelNode, valueNode);
        return card;
    }

    /**
     * Extracts the value Label from a StatCard VBox for live updates.
     *
     * @param statCard the VBox returned by {@link #of(String, String, Paint)}
     * @return the value Label (second child)
     */
    public static Label valueLabel(VBox statCard) {
        return (Label) statCard.getChildren().get(1);
    }

    private static String getAccentClass(Paint paint) {
        if (paint instanceof Color c) {
            int r = (int) (c.getRed() * 255);
            int g = (int) (c.getGreen() * 255);
            int b = (int) (c.getBlue() * 255);
            
            // Match to predefined accent colors
            if (r == 0x3F && g == 0x3D && b == 0x99) return "stat-card-indigo";      // #3F3D99
            if (r == 0x14 && g == 0xB8 && b == 0xA6) return "stat-card-teal";         // #14B8A6
            if (r == 0xF5 && g == 0x9E && b == 0x0B) return "stat-card-amber";        // #F59E0B
            if (r == 0xE1 && g == 0x1D && b == 0x48) return "stat-card-rose";         // #E11D48
            if (r == 0x63 && g == 0x66 && b == 0xF1) return "stat-card-indigo-light"; // #6366F1
        }
        return null;
    }
}