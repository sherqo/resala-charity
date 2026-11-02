package com.resala.gui.views;

import com.resala.controller.VolunteerController;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.ModalOverlay;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.Event;
import com.resala.model.Skill;
import com.resala.model.Volunteer;
import com.resala.model.VolunteerEventAssignment;
import com.resala.volunteer.helpers.VolunteerSummary;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Volunteers view - manage volunteers, assignments, hours, certificates.
 */
public class VolunteerView extends BaseView {

    private final AppShell appShell;
    private final VolunteerController volunteerController;

    private final DataTable<Volunteer> volunteersTable;

    @SuppressWarnings("unchecked")
    public VolunteerView(AppShell appShell, VolunteerController volunteerController) {
        super(createToolbar(), createVolunteersTable());

        this.appShell = appShell;
        this.volunteerController = volunteerController;
        this.volunteersTable = (DataTable<Volunteer>) this.table;

        Button assignBtn = Toolbar.ghost("Assign to Event", e -> showAssignDialog());
        Button logHoursBtn = Toolbar.ghost("Log Hours", e -> showLogHoursDialog());
        Button certBtn = Toolbar.ghost("Certificate", e -> showCertificateDialog());
        Button deleteBtn = Toolbar.ghost("Delete", e -> deleteSelected());
        toolbar.getChildren().addAll(assignBtn, logHoursBtn, certBtn, deleteBtn);

        this.primaryActionButton = Toolbar.primary("Add Volunteer", e -> showAddVolunteerDialog());
    }

    private Button primaryActionButton;

    @Override
    public Button getPrimaryAction() {
        return primaryActionButton;
    }

    private static HBox createToolbar() {
        return Toolbar.of();
    }

    private static DataTable<Volunteer> createVolunteersTable() {
        return DataTable.<Volunteer>builder()
            .column("ID", 60, (java.util.function.Function<Volunteer, String>) v -> String.valueOf(v.getId()))
            .column("Name", 180, Volunteer::getName)
            .column("Email", 200, v -> v.getEmail() != null ? v.getEmail() : "")
            .column("Phone", 150, v -> v.getPhone() != null ? v.getPhone() : "")
            .column("Available", 80, v -> v.isAvailable() ? "Yes" : "No")
            .items(FXCollections.observableArrayList())
            .build();
    }

    private void showAddVolunteerDialog() {
        var fields = List.of(
            new FormDialog.Field("name", "Name", FormDialog.Type.TEXT, true),
            new FormDialog.Field("email", "Email", FormDialog.Type.TEXT, false),
            new FormDialog.Field("phone", "Phone", FormDialog.Type.TEXT, false),
            new FormDialog.Field("available", "Available", FormDialog.Type.CHECKBOX, false, "Available for assignments")
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Add Volunteer", fields);
        result.ifPresent(map -> {
            try {
                String name = (String) map.get("name");
                String email = (String) map.get("email");
                String phone = (String) map.get("phone");
                boolean available = (Boolean) map.get("available");

                Volunteer volunteer = new Volunteer();
                volunteer.setName(name);
                volunteer.setEmail(email.isBlank() ? null : email);
                volunteer.setPhone(phone.isBlank() ? null : phone);
                volunteer.setAvailable(available);

                volunteerController.create(volunteer);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Volunteer added successfully!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showAssignDialog() {
        Volunteer selected = volunteersTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a volunteer first.", Toast.Type.WARNING);
            return;
        }

        List<Event> events = volunteerController.listEvents();
        List<Skill> skills = volunteerController.listSkills();

        if (events.isEmpty() || skills.isEmpty()) {
            appShell.getToast().show(appShell.getStage(), "Need events and skills first.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("eventId", "Event", FormDialog.Type.COMBO, true,
                events.stream().map(e -> e.getId() + ": " + e.getTitle()).toArray()),
            new FormDialog.Field("skillId", "Skill", FormDialog.Type.COMBO, true,
                skills.stream().map(s -> s.getId() + ": " + s.getName()).toArray())
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Assign Volunteer to Event", fields);
        result.ifPresent(map -> {
            try {
                int eventId = Integer.parseInt(((String) map.get("eventId")).split(":")[0]);
                int skillId = Integer.parseInt(((String) map.get("skillId")).split(":")[0]);

                volunteerController.assign(selected.getId(), eventId, skillId);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Volunteer assigned!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showLogHoursDialog() {
        Volunteer selected = volunteersTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a volunteer first.", Toast.Type.WARNING);
            return;
        }

        List<VolunteerEventAssignment> assignments = volunteerController.listAssignments(selected.getId());
        if (assignments.isEmpty()) {
            appShell.getToast().show(appShell.getStage(), "No assignments for this volunteer.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("assignmentId", "Assignment", FormDialog.Type.COMBO, true,
                assignments.stream().map(a -> a.getId() + ": " + a.getEventTitle() + " (" + a.getSkillName() + ")").toArray()),
            new FormDialog.Field("hours", "Hours", FormDialog.Type.NUMBER, true)
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Log Hours", fields);
        result.ifPresent(map -> {
            try {
                int assignmentId = Integer.parseInt(((String) map.get("assignmentId")).split(":")[0]);
                int hours = Integer.parseInt(map.get("hours").toString());

                volunteerController.logHours(assignmentId, hours);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Hours logged!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showCertificateDialog() {
        Volunteer selected = volunteersTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a volunteer first.", Toast.Type.WARNING);
            return;
        }

        try {
            String cert = volunteerController.certificate(selected.getId());
            showCertificatePreview(cert);
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void showCertificatePreview(String text) {
        if (appShell != null && appShell.getModalOverlay() != null) {
            ModalOverlay.show(appShell, new ModalOverlay.ModalCard<Void>() {
                private java.util.function.Consumer<Void> closeCallback;

                @Override
                public Node getNode() {
                    VBox card = new VBox();
                    card.getStyleClass().add("modal-card");
                    card.setMaxWidth(560);
                    card.setMinWidth(480);
                    card.setMaxHeight(560);

                    // Header
                    HBox header = new HBox();
                    header.getStyleClass().add("modal-header");
                    header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label titleLabel = new Label("Volunteer Certificate");
                    titleLabel.getStyleClass().add("modal-title");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button closeBtn = new Button("✕");
                    closeBtn.getStyleClass().add("modal-close-btn");
                    closeBtn.setOnAction(e -> {
                        if (closeCallback != null) closeCallback.accept(null);
                    });

                    header.getChildren().addAll(titleLabel, spacer, closeBtn);

                    // Body
                    VBox body = new VBox(10);
                    body.getStyleClass().add("modal-body");

                    TextArea area = new TextArea(text);
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.getStyleClass().add("preview-area");
                    VBox.setVgrow(area, Priority.ALWAYS);
                    body.getChildren().add(area);
                    VBox.setVgrow(body, Priority.ALWAYS);

                    // Footer
                    HBox footer = new HBox(8);
                    footer.getStyleClass().add("modal-footer");
                    footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                    Region footerSpacer = new Region();
                    HBox.setHgrow(footerSpacer, Priority.ALWAYS);

                    Button doneBtn = new Button("Close");
                    doneBtn.getStyleClass().addAll("btn", "btn-ghost");
                    doneBtn.setOnAction(e -> {
                        if (closeCallback != null) closeCallback.accept(null);
                    });

                    footer.getChildren().addAll(footerSpacer, doneBtn);
                    card.getChildren().addAll(header, body, footer);
                    return card;
                }

                @Override
                public void setCloseHandler(java.util.function.Consumer<Void> closeCallback) {
                    this.closeCallback = closeCallback;
                }
            });
            return;
        }

        // Fallback
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(appShell.getStage());
        dialog.setTitle("Volunteer Certificate");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(new TextArea(text));
        dialog.showAndWait();
    }

    private void deleteSelected() {
        Volunteer selected = volunteersTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a volunteer first.", Toast.Type.WARNING);
            return;
        }

        if (appShell.getConfirmDialog().ask(appShell.getStage(),
                "Delete volunteer '" + selected.getName() + "'?")) {
            volunteerController.delete(selected.getId());
            appShell.refreshAll();
            appShell.getToast().show(appShell.getStage(), "Volunteer deleted.", Toast.Type.SUCCESS);
        }
    }

    @Override
    public void refresh() {
        List<Volunteer> volunteers = volunteerController.listVolunteers();
        volunteersTable.getTableView().setItems(FXCollections.observableArrayList(volunteers));
    }
}