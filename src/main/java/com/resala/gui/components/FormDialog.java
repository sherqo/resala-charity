package com.resala.gui.components;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;

/**
 * Declarative form dialog with validation.
 * Usage:
 * <pre>
 * List<FormDialog.Field> fields = List.of(
 *     new FormDialog.Field("name", "Name", FormDialog.Type.TEXT, true),
 *     new FormDialog.Field("email", "Email", FormDialog.Type.TEXT, false),
 *     new FormDialog.Field("type", "Type", FormDialog.Type.COMBO, "Online", "Check", "In-Kind")
 * );
 * Optional<Map<String, Object>> result = FormDialog.show(stage, "Add Donation", fields);
 * result.ifPresent(map -> { ... });
 * </pre>
 */
public final class FormDialog {

    private FormDialog() {}

    /**
     * Field definition for the form.
     *
     * @param key       unique field identifier (used as map key in result)
     * @param label     display label
     * @param type      field type
     * @param required  whether the field is required
     * @param options   type-specific options (e.g., combo items, min/max for spinner)
     */
    public record Field(String key, String label, Type type, boolean required, Object... options) {}

    public enum Type {
        TEXT, NUMBER, COMBO, DATE, SPINNER, TEXTAREA, CHECKBOX, RADIO_GROUP
    }

    /**
     * Shows a modal form dialog.
     *
     * @param owner  owner window
     * @param title  dialog title
     * @param fields list of field definitions
     * @return Optional containing field values map, or empty if cancelled
     */
    public static Optional<Map<String, Object>> show(Window owner, String title, List<Field> fields) {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
                FormDialog.class.getResource("/styles.css").toExternalForm());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        Map<String, Node> controls = new LinkedHashMap<>();
        Map<String, Field> fieldDefs = new LinkedHashMap<>();

        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            fieldDefs.put(field.key(), field);

            Label label = new Label(field.label() + (field.required() ? " *" : ""));
            label.getStyleClass().add("form-label");
            grid.add(label, 0, i);

            Node control = createControl(field);
            if (control instanceof Control c) {
                c.getStyleClass().add("input");
            }
            grid.add(control, 1, i);
            GridPane.setHgrow(control, javafx.scene.layout.Priority.ALWAYS);
            controls.put(field.key(), control);
        }

        dialog.getDialogPane().setContent(grid);

        // Validation on OK
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            if (!validate(fields, controls)) {
                e.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Map<String, Object> result = new LinkedHashMap<>();
                for (Field field : fields) {
                    result.put(field.key(), extractValue(field, controls.get(field.key())));
                }
                return result;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static Node createControl(Field field) {
        return switch (field.type()) {
            case TEXT -> {
                TextField tf = new TextField();
                if (field.options().length > 0 && field.options()[0] instanceof String placeholder) {
                    tf.setPromptText(placeholder);
                }
                yield tf;
            }
            case NUMBER -> {
                TextField tf = new TextField();
                tf.setPromptText("0");
                yield tf;
            }
            case COMBO -> {
                ComboBox<String> cb = new ComboBox<>();
                for (Object opt : field.options()) {
                    cb.getItems().add(opt.toString());
                }
                if (!cb.getItems().isEmpty()) cb.getSelectionModel().selectFirst();
                yield cb;
            }
            case DATE -> {
                DatePicker dp = new DatePicker();
                dp.setPromptText("YYYY-MM-DD");
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
                ta.setPrefRowCount(4);
                ta.setWrapText(true);
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
                VBox box = new VBox(4);
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
                String value = extractValue(field, control).toString();
                if (value == null || value.trim().isEmpty()) {
                    showValidationError(control, field.label() + " is required");
                    return false;
                }
            }
            if (field.type() == Type.NUMBER) {
                TextField tf = (TextField) control;
                if (!tf.getText().isBlank()) {
                    try {
                        Double.parseDouble(tf.getText());
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
        tooltip.show(control, control.localToScreen(0, 0).getX(), control.localToScreen(0, 0).getY());
        // Remove error style on focus
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
        return switch (field.type()) {
            case TEXT, NUMBER, TEXTAREA -> ((TextInputControl) control).getText();
            case COMBO -> ((ComboBox<?>) control).getValue();
            case DATE -> {
                LocalDate date = ((DatePicker) control).getValue();
                yield date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : "";
            }
            case SPINNER -> ((Spinner<?>) control).getValue();
            case CHECKBOX -> ((CheckBox) control).isSelected();
            case RADIO_GROUP -> {
                ToggleGroup tg = (ToggleGroup) ((VBox) control).getUserData();
                Toggle selected = tg.getSelectedToggle();
                yield selected != null ? selected.getUserData() : null;
            }
        };
    }
}