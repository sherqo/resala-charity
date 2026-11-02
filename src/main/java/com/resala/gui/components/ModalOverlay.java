package com.resala.gui.components;

import com.resala.gui.AppShell;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * In-app modal overlay manager.
 * Renders centered modal cards inside the application window with a translucent backdrop,
 * preventing new floating OS windows and window overflow.
 */
public final class ModalOverlay {

    private ModalOverlay() {}

    public interface ModalCard<T> {
        Node getNode();
        void setCloseHandler(Consumer<T> closeCallback);
        default void onOpened() {}
    }

    /**
     * Displays an in-app modal card synchronously using a nested event loop.
     * Blocks caller thread until dismissed, while keeping the JavaFX UI reactive.
     */
    public static <T> T show(AppShell appShell, ModalCard<T> card) {
        if (appShell == null) return null;
        StackPane overlay = appShell.getModalOverlay();
        if (overlay == null) return null;

        String loopKey = "MODAL_" + UUID.randomUUID();
        AtomicReference<T> resultRef = new AtomicReference<>(null);
        AtomicBoolean closed = new AtomicBoolean(false);

        // Semi-transparent backdrop
        Region backdrop = new Region();
        backdrop.getStyleClass().add("modal-backdrop");
        backdrop.prefWidthProperty().bind(overlay.widthProperty());
        backdrop.prefHeightProperty().bind(overlay.heightProperty());

        Node cardNode = card.getNode();

        // Dismiss action
        final javafx.event.EventHandler<KeyEvent>[] escHandlerRef = new javafx.event.EventHandler[1];

        Runnable dismiss = () -> {
            if (closed.getAndSet(true)) return;
            overlay.setVisible(false);
            overlay.setManaged(false);
            overlay.getChildren().clear();

            if (appShell.getStage() != null && appShell.getStage().getScene() != null && escHandlerRef[0] != null) {
                appShell.getStage().getScene().removeEventFilter(KeyEvent.KEY_PRESSED, escHandlerRef[0]);
            }
            Platform.exitNestedEventLoop(loopKey, resultRef.get());
        };

        card.setCloseHandler(val -> {
            resultRef.set(val);
            dismiss.run();
        });

        backdrop.setOnMouseClicked(e -> {
            resultRef.set(null);
            dismiss.run();
        });

        escHandlerRef[0] = e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                resultRef.set(null);
                dismiss.run();
            }
        };

        overlay.getChildren().setAll(backdrop, cardNode);
        StackPane.setAlignment(cardNode, Pos.CENTER);
        overlay.setVisible(true);
        overlay.setManaged(true);
        overlay.toFront();

        if (appShell.getStage() != null && appShell.getStage().getScene() != null) {
            appShell.getStage().getScene().addEventFilter(KeyEvent.KEY_PRESSED, escHandlerRef[0]);
        }

        Platform.runLater(card::onOpened);

        Object res = Platform.enterNestedEventLoop(loopKey);
        return (T) res;
    }
}
