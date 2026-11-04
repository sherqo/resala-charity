package com.resala.gui.views;

import com.resala.controller.VolunteerController;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.ModalOverlay;
import com.resala.gui.components.PreviewDialog;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.Event;
import com.resala.model.Skill;
import com.resala.model.Volunteer;
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

        Button editBtn = Toolbar.ghost("Edit", e -> showEditVolunteerDialog());
        Button availabilityBtn = Toolbar.ghost("Availability", e -> toggleAvailability());
        Button skillsBtn = Toolbar.ghost("Skills", e -> showSkillsDialog());
        Button hoursBtn = Toolbar.ghost("Hours", e -> showHoursDialog());
        Button assignBtn = Toolbar.ghost("Register to Event", e -> showAssignDialog());
        Button certBtn = Toolbar.ghost("Print Certificate", e -> showCertificateDialog());
        Button deleteBtn = Toolbar.ghost("Delete", e -> deleteSelected());
        toolbar.getChildren().addAll(editBtn, availabilityBtn, skillsBtn, hoursBtn, assignBtn, certBtn, deleteBtn);

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

        if (!selected.isAvailable()) {
            appShell.getToast().show(appShell.getStage(),
                "This volunteer is not available for assignment.", Toast.Type.WARNING);
            return;
        }

        List<Event> events = volunteerController.listOpenEvents();
        List<Skill> skills = volunteerController.getVolunteerSkills(selected.getId());

        if (events.isEmpty() || skills.isEmpty()) {
            appShell.getToast().show(appShell.getStage(),
                "No open events available, or this volunteer has no skills.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("eventId", "Event", FormDialog.Type.COMBO, true,
                events.stream().map(e -> e.getId() + ": " + e.getTitle() + " (" + e.getHours() + " hrs)").toArray()),
            new FormDialog.Field("skillId", "Skill", FormDialog.Type.COMBO, true,
                skills.stream().map(s -> s.getId() + ": " + s.getName()).toArray())
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Register Volunteer to Event", fields);
        result.ifPresent(map -> {
            try {
                int eventId = Integer.parseInt(((String) map.get("eventId")).split(":")[0]);
                int skillId = Integer.parseInt(((String) map.get("skillId")).split(":")[0]);

                volunteerController.assign(selected.getId(), eventId, skillId);
                var reg = volunteerController.getEventRegistration(selected.getId(), eventId);
                String ticketInfo = reg.map(r -> " (Ticket: " + r.getTicketRef() + ")").orElse("");
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Volunteer registered to event!" + ticketInfo
                        + " Hours are credited when the volunteer checks in.",
                    Toast.Type.SUCCESS);
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
        PreviewDialog.show(appShell, "Volunteer Certificate", text);
    }

    private Volunteer selectedVolunteer() {
        Volunteer selected = volunteersTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a volunteer first.", Toast.Type.WARNING);
        }
        return selected;
    }

    private void showEditVolunteerDialog() {
        Volunteer selected = selectedVolunteer();
        if (selected == null) return;

        Volunteer current = volunteerController.getVolunteer(selected.getId());
        // COMBO selects the first item, so put the current value first.
        String[] availability = current.isAvailable()
            ? new String[]{"Yes", "No"}
            : new String[]{"No", "Yes"};

        var fields = List.of(
            new FormDialog.Field("name", "Name", FormDialog.Type.TEXT, true, current.getName()),
            new FormDialog.Field("email", "Email", FormDialog.Type.TEXT, false,
                current.getEmail() == null ? "" : current.getEmail()),
            new FormDialog.Field("phone", "Phone", FormDialog.Type.TEXT, false,
                current.getPhone() == null ? "" : current.getPhone()),
            new FormDialog.Field("available", "Available", FormDialog.Type.COMBO, true, (Object[]) availability)
        );

        Optional<java.util.Map<String, Object>> result =
            FormDialog.show(appShell.getStage(), "Edit Volunteer", fields);
        result.ifPresent(map -> {
            try {
                String name = (String) map.get("name");
                String email = (String) map.get("email");
                String phone = (String) map.get("phone");
                boolean available = "Yes".equals(map.get("available"));

                Volunteer volunteer = volunteerController.getVolunteer(selected.getId());
                volunteer.setName(name);
                volunteer.setEmail(email == null || email.isBlank() ? null : email.trim());
                volunteer.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
                volunteer.setAvailable(available);

                volunteerController.update(selected.getId(), volunteer);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Volunteer updated.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void toggleAvailability() {
        Volunteer selected = selectedVolunteer();
        if (selected == null) return;

        try {
            boolean available = !selected.isAvailable();
            volunteerController.setAvailability(selected.getId(), available);
            appShell.refreshAll();
            appShell.getToast().show(appShell.getStage(),
                selected.getName() + " is now " + (available ? "available" : "unavailable") + ".",
                Toast.Type.SUCCESS);
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void showSkillsDialog() {
        Volunteer selected = selectedVolunteer();
        if (selected == null) return;

        List<Skill> skills = volunteerController.getVolunteerSkills(selected.getId());
        StringBuilder current = new StringBuilder("Skills for " + selected.getName() + ":
");
        if (skills.isEmpty()) {
            current.append("(none)
");
        } else {
            for (Skill skill : skills) {
                current.append("#").append(skill.getId()).append("  ").append(skill.getName()).append("
");
            }
        }
        current.append("
Available skills:
");
        for (Skill skill : volunteerController.listSkills()) {
            current.append("#").append(skill.getId()).append("  ").append(skill.getName()).append("
");
        }
        PreviewDialog.show(appShell, "Volunteer Skills", current.toString());

        var fields = List.of(
            new FormDialog.Field("addSkillId", "Skill ID to add (blank = skip)", FormDialog.Type.TEXT, false),
            new FormDialog.Field("removeSkillId", "Skill ID to remove (blank = skip)", FormDialog.Type.TEXT, false)
        );

        Optional<java.util.Map<String, Object>> result =
            FormDialog.show(appShell.getStage(), "Change Skills", fields);
        result.ifPresent(map -> {
            try {
                String add = (String) map.get("addSkillId");
                String remove = (String) map.get("removeSkillId");
                boolean changed = false;

                if (add != null && !add.isBlank()) {
                    volunteerController.addSkill(selected.getId(), Integer.parseInt(add.trim()));
                    changed = true;
                }
                if (remove != null && !remove.isBlank()) {
                    volunteerController.removeSkill(selected.getId(), Integer.parseInt(remove.trim()));
                    changed = true;
                }

                appShell.refreshAll();
                if (changed) {
                    appShell.getToast().show(appShell.getStage(), "Skills updated.", Toast.Type.SUCCESS);
                } else {
                    appShell.getToast().show(appShell.getStage(), "No skill change.", Toast.Type.INFO);
                }
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showHoursDialog() {
        Volunteer selected = selectedVolunteer();
        if (selected == null) return;

        VolunteerSummary summary = volunteerController.summary(selected.getId());
        StringBuilder text = new StringBuilder();
        text.append("Total hours: ").append(summary.getTotalHours()).append("

");
        if (summary.getHoursByEvent().isEmpty()) {
            text.append("No hours recorded yet.");
        } else {
            for (var entry : summary.getHoursByEvent().entrySet()) {
                text.append(entry.getKey()).append(": ").append(entry.getValue()).append("
");
            }
        }
        PreviewDialog.show(appShell, "Volunteer Hours", text.toString());
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
