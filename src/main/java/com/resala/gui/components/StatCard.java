package com.resala.gui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;

public final class StatCard {

    private StatCard() {}

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

    public static VBox of(String label, String value, Paint accent) {
        return of(label, value);
    }

    public static Label valueLabel(VBox statCard) {
        return (Label) statCard.getChildren().get(1);
    }
}
