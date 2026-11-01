package com.resala.gui.views;

import com.resala.controller.VolunteerController;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.EmptyState;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
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

    private final AppShell appShell;
    private final VolunteerController volunteerController;

    private DataTable<Skill> lookupTable;
    private ComboBox<String> tableSelector;
    private String currentTable = "skills";

    public LookupView(AppShell appShell, VolunteerController volunteerController) {
        super(createToolbar(), createLookupTable());

        this.appShell = appShell;
        this.volunteerController = volunteerController;

        this.lookupTable = (DataTable<Skill>) getTableView().getProperties().get("dataTable");
        if (this.lookupTable == null) {
            this.lookupTable = createLookupTable();
            getTableView().getProperties().put("dataTable", this.lookupTable);
        }

        this.primaryActionButton = Toolbar.primary("+ Add", e -> showAddDialog());

        buildTableSelector();
    }

    private Button primaryActionButton;

    @Override
    public Button getPrimaryAction() {
        return primaryActionButton;
    }

    private static HBox createToolbar() {
        return Toolbar.of();
    }

    private void buildTableSelector() {
        tableSelector = new ComboBox<>();
        tableSelector.getItems().addAll("Skills", "Event Types", "Need Types", "Donation Types");
        tableSelector.getSelectionModel().selectFirst();
        tableSelector.getStyleClass().add("input");
        tableSelector.setPrefWidth(200);

        tableSelector.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            if (nw != null) {
                switch (nw) {
                    case "Skills" -> currentTable = "skills";
                    case "Event Types" -> currentTable = "event_types";
                    case "Need Types" -> currentTable = "need_types";
                    case "Donation Types" -> currentTable = "donation_types";
                }
                refresh();
            }
        });

        // Add to toolbar area
        HBox selectorBox = new HBox(12, new Label("Table:"), tableSelector);
        selectorBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        getChildren().add(0, selectorBox);
    }

    private static DataTable<Skill> createLookupTable() {
        return DataTable.<Skill>builder()
            .column("ID", 60, (java.util.function.Function<Skill, String>) s -> String.valueOf(s.getId()))
            .column("Name", 300, Skill::getName)
            .items(FXCollections.observableArrayList())
            .build();
    }

    private void showAddDialog() {
        var fields = List.of(
            new FormDialog.Field("name", "Name", FormDialog.Type.TEXT, true)
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Add " + currentTable.substring(0, 1).toUpperCase() + currentTable.substring(1).replace("_", " "), fields);
        result.ifPresent(map -> {
            try {
                String name = (String) map.get("name");
                if ("skills".equals(currentTable)) {
                    volunteerController.createSkill(name);
                } else {
                    appShell.getToast().show(appShell.getStage(),
                        "Only Skills table is editable in this demo.", Toast.Type.INFO);
                    return;
                }
                refresh();
                appShell.getToast().show(appShell.getStage(), "Added!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showRenameDialog() {
        Skill selected = lookupTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an item first.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("name", "New Name", FormDialog.Type.TEXT, true, selected.getName())
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Rename", fields);
        result.ifPresent(map -> {
            appShell.getToast().show(appShell.getStage(),
                "Rename not fully implemented for all tables.", Toast.Type.INFO);
        });
    }

    private void deleteSelected() {
        Skill selected = lookupTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an item first.", Toast.Type.WARNING);
            return;
        }

        if (appShell.getConfirmDialog().ask(appShell.getStage(),
                "Delete '" + selected.getName() + "'?")) {
            try {
                if ("skills".equals(currentTable)) {
                    volunteerController.deleteSkill(selected.getId());
                } else {
                    appShell.getToast().show(appShell.getStage(),
                        "Only Skills table is editable in this demo.", Toast.Type.INFO);
                    return;
                }
                refresh();
                appShell.getToast().show(appShell.getStage(), "Deleted.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        }
    }

    @Override
    public void refresh() {
        List<Skill> skills = volunteerController.listSkills();
        lookupTable.getTableView().setItems(FXCollections.observableArrayList(skills));
    }
}