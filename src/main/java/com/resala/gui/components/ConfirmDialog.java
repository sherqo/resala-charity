package com.resala.gui.components;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/**
 * Confirmation dialog utility.
 */
public class ConfirmDialog {

    /**
     * Shows a confirmation dialog with Yes/No buttons.
     *
     * @param owner   the owner window (Stage or any Window)
     * @param message the confirmation message
     * @return true if user clicked Yes, false otherwise
     */
    public boolean ask(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("Confirm");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        // Style the buttons
        alert.getDialogPane().getStylesheets().add(
                ConfirmDialog.class.getResource("/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("confirm-dialog");

        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    /**
     * Shows a confirmation dialog with custom title.
     *
     * @param owner   the owner window
     * @param title   the dialog title
     * @param message the confirmation message
     * @return true if user clicked Yes, false otherwise
     */
    public boolean ask(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}