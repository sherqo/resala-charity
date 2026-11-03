package com.resala.gui.components;

import com.resala.gui.AppShell;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public final class FormDialog {

    private FormDialog() {}

    public record Field(String key, String label, Type type, boolean required, Object... options) {}

    public enum Type {
        TEXT, NUMBER, COMBO, DATE, SPINNER, TEXTAREA, CHECKBOX, RADIO_GROUP
    }

    public static Optional<Map<String, Object>> show(Window owner, String title, List<Field> fields) {
        AppShell appShell = null;
        if (owner != null && owner.getScene() != null && owner.getScene().getUserData() instanceof AppShell shell) {
            appShell = shell;
        } else {
            appShell = AppShell.getInstance();
        }

        if (appShell != null && appShell.getModalOverlay() != null) {
            return showInAppModal(appShell, title, fields);
        }

        // Fallback for standalone headless/dialog tests without AppShell
        return showFallbackDialog(owner, title, fields);
    }

    private static Optional<Map<String, Object>> showInAppModal(AppShell appShell, String title, List<Field> fields) {
        return ModalOverlay.show(appShell, new ModalOverlay.ModalCard<>() {
            private Consumer<Optional<Map<String, Object>>> closeCallback;
            private Node firstControl;
            private final Map<String, Node> controls = new LinkedHashMap<>();

            @Override
            public Node getNode() {
                VBox card = new VBox();
                card.getStyleClass().add("modal-card");
                card.setMaxWidth(480);
                card.setMinWidth(420);

                // Bound max height so modal NEVER overflows the window
                double maxH = 580;
                if (appShell.getStage() != null && appShell.getStage().getHeight() > 200) {
                    maxH = Math.min(620, appShell.getStage().getHeight() * 0.85);
                }
                card.setMaxHeight(maxH);

                // Header
                HBox header = new HBox();
                header.getStyleClass().add("modal-header");
                header.setAlignment(Pos.CENTER_LEFT);

                Label titleLabel = new Label(title);
                titleLabel.getStyleClass().add("modal-title");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button closeBtn = new Button("✕");
                closeBtn.getStyleClass().add("modal-close-btn");
                closeBtn.setOnAction(e -> {
                    if (closeCallback != null) closeCallback.accept(Optional.empty());
                });

                header.getChildren().addAll(titleLabel, spacer, closeBtn);

                // Body with vertical stacked fields
                VBox formBox = new VBox(14);
                formBox.getStyleClass().add("modal-body");

                for (Field field : fields) {
                    Node control = createControl(field);
                    if (firstControl == null && !(control instanceof CheckBox)) {
                        firstControl = control;
                    }

                    if (control instanceof Region r) {
                        r.setMaxWidth(Double.MAX_VALUE);
                    }
                    if (control instanceof TextField tf) {
                        tf.getStyleClass().add("input");
                        // Submit on ENTER
                        tf.setOnKeyPressed(ke -> {
                            if (ke.getCode() == KeyCode.ENTER) {
                                submit();
                            }
                        });
                    }

                    VBox fieldBox = new VBox(5);
                    fieldBox.getStyleClass().add("dialog-form-field");

                    if (field.type() == Type.CHECKBOX) {
                        fieldBox.getChildren().add(control);
                    } else {
                        HBox labelBox = new HBox(4);
                        labelBox.setAlignment(Pos.CENTER_LEFT);

                        Label label = new Label(field.label());
                        label.getStyleClass().add("dialog-field-label");
                        labelBox.getChildren().add(label);

                        if (field.required()) {
                            Label req = new Label("*");
                            req.getStyleClass().add("dialog-required-mark");
                            labelBox.getChildren().add(req);
                        }
                        fieldBox.getChildren().addAll(labelBox, control);
                    }

                    formBox.getChildren().add(fieldBox);
                    controls.put(field.key(), control);
                }

                // Scroll pane wrapping form box
                ScrollPane scroll = new ScrollPane(formBox);
                scroll.getStyleClass().add("modal-scroll");
                scroll.setFitToWidth(true);
                scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                VBox.setVgrow(scroll, Priority.ALWAYS);

                // Footer
                HBox footer = new HBox(8);
                footer.getStyleClass().add("modal-footer");
                footer.setAlignment(Pos.CENTER_RIGHT);

                Region footerSpacer = new Region();
                HBox.setHgrow(footerSpacer, Priority.ALWAYS);

                Button cancelBtn = new Button("Cancel");
                cancelBtn.getStyleClass().addAll("btn", "btn-ghost");
                cancelBtn.setOnAction(e -> {
                    if (closeCallback != null) closeCallback.accept(Optional.empty());
                });

                String actionName = title.startsWith("Add ") || title.startsWith("Create ") ? title : "Confirm";
                if (title.contains("Card Details") || title.contains("Wallet Details")) actionName = "Continue";
                if (title.startsWith("Register")) actionName = "Register";
                if (title.startsWith("Reschedule")) actionName = "Reschedule";

                Button submitBtn = new Button(actionName);
                submitBtn.getStyleClass().addAll("btn", "btn-primary");
                submitBtn.setOnAction(e -> submit());

                footer.getChildren().addAll(footerSpacer, cancelBtn, submitBtn);
                card.getChildren().addAll(header, scroll, footer);
                return card;
            }

            private void submit() {
                if (!validate(fields, controls)) {
                    return;
                }
                Map<String, Object> data = new LinkedHashMap<>();
                for (Field f : fields) {
                    data.put(f.key(), extractValue(f, controls.get(f.key())));
                }
                if (closeCallback != null) {
                    closeCallback.accept(Optional.of(data));
                }
            }

            @Override
            public void setCloseHandler(Consumer<Optional<Map<String, Object>>> closeCallback) {
                this.closeCallback = closeCallback;
            }

            @Override
            public void onOpened() {
                if (firstControl != null) {
                    firstControl.requestFocus();
                }
            }
        });
    }

    private static Optional<Map<String, Object>> showFallbackDialog(Window owner, String title, List<Field> fields) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(FormDialog.class.getResource("/styles.css").toExternalForm());

        VBox formBox = new VBox(14);
        Map<String, Node> controls = new LinkedHashMap<>();
        for (Field f : fields) {
            Node c = createControl(f);
            formBox.getChildren().addAll(new Label(f.label()), c);
            controls.put(f.key(), c);
        }
        dialog.getDialogPane().setContent(formBox);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Map<String, Object> data = new LinkedHashMap<>();
                for (Field f : fields) data.put(f.key(), extractValue(f, controls.get(f.key())));
                return data;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private static Node createControl(Field field) {
        return switch (field.type()) {
            case TEXT -> {
                TextField tf = new TextField();
                if (field.options().length > 0 && field.options()[0] instanceof String init) {
                    tf.setText(init);
                }
                yield tf;
            }
            case NUMBER -> {
                TextField tf = new TextField();
                tf.setPromptText("0");
                if (field.options().length > 0 && field.options()[0] != null) {
                    tf.setText(field.options()[0].toString());
                }
                yield tf;
            }
            case COMBO -> {
                ComboBox<String> cb = new ComboBox<>();
                for (Object opt : field.options()) {
                    if (opt instanceof Object[] arr) {
                        for (Object item : arr) cb.getItems().add(item.toString());
                    } else if (opt instanceof Collection<?> col) {
                        for (Object item : col) cb.getItems().add(item.toString());
                    } else {
                        cb.getItems().add(opt.toString());
                    }
                }
                if (!cb.getItems().isEmpty()) cb.getSelectionModel().selectFirst();
                yield cb;
            }
            case DATE -> {
                DatePicker dp = new DatePicker();
                dp.setPromptText("YYYY-MM-DD");
                if (field.options().length > 0 && field.options()[0] != null) {
                    if (field.options()[0] instanceof LocalDate ld) {
                        dp.setValue(ld);
                    } else {
                        try {
                            dp.setValue(LocalDate.parse(field.options()[0].toString()));
                        } catch (Exception ignored) {}
                    }
                }
                yield dp;
            }
            case SPINNER -> {
                int min = field.options().length > 0 ? ((Number) field.options()[0]).intValue() : 0;
                int max = field.options().length > 1 ? ((Number) field.options()[1]).intValue() : 100;
                int initial = field.options().length > 2 ? ((Number) field.options()[2]).intValue() : min;
                Spinner<Integer> sp = new Spinner<>(min, max, initial);
                sp.setEditable(true);
                yield sp;
            }
            case TEXTAREA -> {
                TextArea ta = new TextArea();
                ta.setPrefRowCount(3);
                ta.setWrapText(true);
                if (field.options().length > 0 && field.options()[0] != null) {
                    ta.setText(field.options()[0].toString());
                }
                yield ta;
            }
            case CHECKBOX -> {
                CheckBox cb = new CheckBox();
                if (field.options().length > 0 && field.options()[0] instanceof String label) {
                    cb.setText(label);
                }
                yield cb;
            }
            case RADIO_GROUP -> {
                ToggleGroup tg = new ToggleGroup();
                VBox box = new VBox(6);
                for (Object opt : field.options()) {
                    RadioButton rb = new RadioButton(opt.toString());
                    rb.setToggleGroup(tg);
                    rb.setUserData(opt.toString());
                    box.getChildren().add(rb);
                }
                box.setUserData(tg);
                yield box;
            }
        };
    }

    private static boolean validate(List<Field> fields, Map<String, Node> controls) {
        for (Field field : fields) {
            Node control = controls.get(field.key());
            if (field.required()) {
                Object val = extractValue(field, control);
                String value = val != null ? val.toString().trim() : "";
                if (value.isEmpty()) {
                    showValidationError(control, field.label() + " is required");
                    return false;
                }
            }
            if (field.type() == Type.NUMBER) {
                TextField tf = (TextField) control;
                if (!tf.getText().isBlank()) {
                    try {
                        Double.parseDouble(tf.getText().trim());
                    } catch (NumberFormatException ex) {
                        showValidationError(control, "Must be a valid number");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void showValidationError(Node control, String message) {
        if (control instanceof Control c) {
            c.getStyleClass().add("input-error");
        }
        Tooltip tooltip = new Tooltip(message);
        Tooltip.install(control, tooltip);
        try {
            tooltip.show(control, control.localToScreen(0, 0).getX(), control.localToScreen(0, 0).getY());
        } catch (Exception ignored) {}
        control.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                if (control instanceof Control c) {
                    c.getStyleClass().remove("input-error");
                }
                Tooltip.uninstall(control, tooltip);
            }
        });
    }

    private static Object extractValue(Field field, Node control) {
        if (control == null) return null;
        return switch (field.type()) {
            case TEXT, NUMBER -> ((TextField) control).getText().trim();
            case COMBO -> {
                @SuppressWarnings("unchecked")
                ComboBox<String> cb = (ComboBox<String>) control;
                yield cb.getValue();
            }
            case DATE -> {
                DatePicker dp = (DatePicker) control;
                LocalDate date = dp.getValue();
                yield date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "";
            }
            case SPINNER -> {
                @SuppressWarnings("unchecked")
                Spinner<Integer> sp = (Spinner<Integer>) control;
                yield sp.getValue();
            }
            case TEXTAREA -> ((TextArea) control).getText().trim();
            case CHECKBOX -> ((CheckBox) control).isSelected();
            case RADIO_GROUP -> {
                VBox box = (VBox) control;
                ToggleGroup tg = (ToggleGroup) box.getUserData();
                Toggle sel = tg.getSelectedToggle();
                yield sel != null ? sel.getUserData() : null;
            }
        };
    }
}
