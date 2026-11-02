package com.resala.gui.views;

import com.resala.controller.EventController;
import com.resala.controller.VolunteerController;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.EventType;
import com.resala.model.Skill;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Lookups view - admin CRUD for lookup tables (Skills, Event Types, Need Types, Donation Types).
 */
public class LookupView extends BaseView {

    // Supported tables
    private enum LookupTable {
        SKILLS("Skills"),
        EVENT_TYPES("Event Types");

        final String label;
        LookupTable(String label) { this.label = label; }

        static LookupTable fromLabel(String label) {
            for (LookupTable t : values()) if (t.label.equals(label)) return t;
            return SKILLS;
        }
    }

    private final AppShell appShell;
    private final VolunteerController volunteerController;
    private final EventController eventController;

    private DataTable<Object> lookupTable;
    private ComboBox<String> tableSelector;
    private LookupTable currentTable = LookupTable.SKILLS;

    private Button primaryActionButton;

    @SuppressWarnings("unchecked")
    public LookupView(AppShell appShell,
                      VolunteerController volunteerController,
                      EventController eventController) {
        super(createToolbar(), createGenericTable());

        this.appShell            = appShell;
        this.volunteerController = volunteerController;
        this.eventController     = eventController;

        this.lookupTable = (DataTable<Object>) getTableView().getProperties().get("dataTable");
        if (this.lookupTable == null) {
            this.lookupTable = createGenericTable();
            getTableView().getProperties().put("dataTable", this.lookupTable);
        }

        this.primaryActionButton = Toolbar.primary("+ Add", e -> showAddDialog());
        buildTableSelector();
    }

    @Override
    public Button getPrimaryAction() { return primaryActionButton; }

    private static HBox createToolbar() { return Toolbar.of(); }

    @SuppressWarnings("unchecked")
    private static DataTable<Object> createGenericTable() {
        return (DataTable<Object>) (DataTable<?>) DataTable.<Skill>builder()
            .column("ID",   60,  (java.util.function.Function<Skill, String>) s -> String.valueOf(s.getId()))
            .column("Name", 300, Skill::getName)
            .items(FXCollections.observableArrayList())
            .build();
    }

    private void buildTableSelector() {
        tableSelector = new ComboBox<>();
        for (LookupTable t : LookupTable.values()) tableSelector.getItems().add(t.label);
        tableSelector.getSelectionModel().selectFirst();
        tableSelector.getStyleClass().add("input");
        tableSelector.setPrefWidth(200);

        tableSelector.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            if (nw != null) {
                currentTable = LookupTable.fromLabel(nw);
                refresh();
            }
        });

        HBox selectorBox = new HBox(12, new Label("Table:"), tableSelector);
        selectorBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        getChildren().add(0, selectorBox);
    }

    // ------------------------------------------------------------------ Add
    private void showAddDialog() {
        var fields = List.of(
            new FormDialog.Field("name", "Name", FormDialog.Type.TEXT, true)
        );
        String title = "Add " + currentTable.label;
        Optional<java.util.Map<String, Object>> result =
            FormDialog.show(appShell.getStage(), title, fields);

        result.ifPresent(map -> {
            try {
                String name = (String) map.get("name");
                switch (currentTable) {
                    case SKILLS      -> volunteerController.createSkill(name);
                    case EVENT_TYPES -> eventController.createEventType(name);
                }
                refresh();
                appShell.getToast().show(appShell.getStage(), "Added!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(),
                    "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    // --------------------------------------------------------------- Rename
    @SuppressWarnings("unchecked")
    private void showRenameDialog() {
        Object selected = ((DataTable<Object>) lookupTable).getTableView()
            .getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(),
                "Select an item first.", Toast.Type.WARNING);
            return;
        }

        String currentName = nameOf(selected);
        var fields = List.of(
            new FormDialog.Field("name", "New Name", FormDialog.Type.TEXT, true, currentName)
        );

        Optional<java.util.Map<String, Object>> result =
            FormDialog.show(appShell.getStage(), "Rename", fields);
        result.ifPresent(map -> {
            try {
                String newName = (String) map.get("name");
                switch (currentTable) {
                    case SKILLS      -> appShell.getToast().show(appShell.getStage(),
                        "Rename not supported for Skills yet.", Toast.Type.INFO);
                    case EVENT_TYPES -> {
                        eventController.renameEventType(idOf(selected), newName);
                        refresh();
                        appShell.getToast().show(appShell.getStage(),
                            "Renamed to \"" + newName + "\".", Toast.Type.SUCCESS);
                    }
                }
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(),
                    "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    // --------------------------------------------------------------- Delete
    @SuppressWarnings("unchecked")
    private void deleteSelected() {
        Object selected = ((DataTable<Object>) lookupTable).getTableView()
            .getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(),
                "Select an item first.", Toast.Type.WARNING);
            return;
        }

        if (!appShell.getConfirmDialog().ask(appShell.getStage(),
                "Delete \"" + nameOf(selected) + "\"?")) return;

        try {
            switch (currentTable) {
                case SKILLS      -> volunteerController.deleteSkill(idOf(selected));
                case EVENT_TYPES -> eventController.deleteEventType(idOf(selected));
            }
            refresh();
            appShell.getToast().show(appShell.getStage(), "Deleted.", Toast.Type.SUCCESS);
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(),
                "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    // --------------------------------------------------------------- Refresh
    @Override
    public void refresh() {
        List<?> items = switch (currentTable) {
            case SKILLS      -> volunteerController.listSkills();
            case EVENT_TYPES -> eventController.listEventTypes();
        };
        lookupTable.getTableView().setItems(
            FXCollections.observableArrayList(items));
    }

    // ------------------------------------------------- Helpers to unbox rows
    private int idOf(Object row) {
        if (row instanceof Skill s)     return s.getId();
        if (row instanceof EventType t) return t.getId();
        throw new IllegalStateException("Unknown row type: " + row.getClass());
    }

    private String nameOf(Object row) {
        if (row instanceof Skill s)     return s.getName();
        if (row instanceof EventType t) return t.getName();
        throw new IllegalStateException("Unknown row type: " + row.getClass());
    }
}
