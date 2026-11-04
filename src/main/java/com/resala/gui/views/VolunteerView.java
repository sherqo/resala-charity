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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class VolunteerView extends BaseView {

    private final AppShell appShell;
    private final VolunteerController volunteerController;

    private final DataTable<Volunteer> volunteersTable;

    private Button primaryActionButton;
    private Integer skillFilterId;

    @SuppressWarnings("unchecked")
    public VolunteerView(AppShell appShell, VolunteerController volunteerController) {
        super(createToolbar(), createVolunteersTable());

        this.appShell = appShell;
        this.volunteerController = volunteerController;
        this.volunteersTable = (DataTable<Volunteer>) this.table;

        Button editBtn = Toolbar.ghost("Edit Volunteer", e -> showEditVolunteerDialog());
        Button availabilityBtn = Toolbar.ghost("Set Availability", e -> showAvailabilityDialog());
        Button assignBtn = Toolbar.ghost("Register to Event", e -> showAssignDialog());
        Button assignmentsBtn = Toolbar.ghost("Assignments", e -> showAssignments());
        Button summaryBtn = Toolbar.ghost("Hours Summary", e -> showHoursSummary());
        Button certBtn = Toolbar.ghost("Print Certificate", e -> showCertificateDialog());
        Button deleteBtn = Toolbar.ghost("Delete", e -> deleteSelected());
        toolbar.getChildren().addAll(editBtn, availabilityBtn, assignBtn, assignmentsBtn, summaryBtn, certBtn, deleteBtn);

        HBox skillsToolbar = Toolbar.of(
            Toolbar.ghost("Show Skills", e -> showVolunteerSkills()),
            Toolbar.ghost("Add Skill", e -> addSkillToVolunteer()),
            Toolbar.ghost("Remove Skill", e -> removeSkillFromVolunteer()),
            Toolbar.ghost("Search by Skill", e -> showSearchBySkillDialog()),
            Toolbar.ghost("Add Skill Type", e -> showAddSkillTypeDialog()),
            Toolbar.ghost("Delete Skill Type", e -> showDeleteSkillTypeDialog())
        );
        getChildren().add(1, skillsToolbar);

        this.primaryActionButton = Toolbar.primary("Add Volunteer", e -> showAddVolunteerDialog());
    }

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

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Add Volunteer", fields);
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

    private void showEditVolunteerDialog() {
        Volunteer selected = requireSelected();
        if (selected == null) return;

        var fields = List.of(
            new FormDialog.Field("name", "Name", FormDialog.Type.TEXT, true,
                selected.getName() != null ? selected.getName() : ""),
            new FormDialog.Field("email", "Email", FormDialog.Type.TEXT, false,
                selected.getEmail() != null ? selected.getEmail() : ""),
            new FormDialog.Field("phone", "Phone", FormDialog.Type.TEXT, false,
                selected.getPhone() != null ? selected.getPhone() : ""),
            new FormDialog.Field("available", "Available", FormDialog.Type.COMBO, true,
                availabilityOptions(selected.isAvailable()))
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Edit Volunteer", fields);
        result.ifPresent(map -> {
            try {
                String name = (String) map.get("name");
                String email = (String) map.get("email");
                String phone = (String) map.get("phone");
                boolean available = "Yes".equals(map.get("available"));

                Volunteer volunteer = new Volunteer(
                    name,
                    email.isBlank() ? null : email,
                    phone.isBlank() ? null : phone,
                    available
                );

                volunteerController.update(selected.getId(), volunteer);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Volunteer updated successfully!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showAvailabilityDialog() {
        Volunteer selected = requireSelected();
        if (selected == null) return;

        var fields = List.of(
            new FormDialog.Field("available", "Available", FormDialog.Type.COMBO, true,
                availabilityOptions(selected.isAvailable()))
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Set Availability", fields);
        result.ifPresent(map -> {
            try {
                boolean available = "Yes".equals(map.get("available"));
                volunteerController.setAvailability(selected.getId(), available);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Availability updated to " + (available ? "available" : "not available") + ".",
                    Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showAssignDialog() {
        Volunteer selected = requireSelected();
        if (selected == null) return;

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

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Register Volunteer to Event", fields);
        result.ifPresent(map -> {
            try {
                int eventId = idFromOption(map.get("eventId"));
                int skillId = idFromOption(map.get("skillId"));

                volunteerController.assign(selected.getId(), eventId, skillId);
                var reg = volunteerController.getEventRegistration(selected.getId(), eventId);
                String ticketInfo = reg.map(r -> " (Ticket: " + r.getTicketRef() + ")").orElse("");
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Volunteer registered to event!" + ticketInfo
                        + " Hours will be credited after the event closes.",
                    Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showAssignments() {
        Volunteer selected = requireSelected();
        if (selected == null) return;

        try {
            List<VolunteerEventAssignment> assignments = volunteerController.listAssignments(selected.getId());
            StringBuilder text = new StringBuilder();
            text.append("Volunteer: ").append(selected.getName()).append("\n");
            text.append("Assignments: ").append(assignments.size()).append("\n\n");

            if (assignments.isEmpty()) {
                text.append("No assignments found.");
            } else {
                text.append(String.format("%-4s %-12s %-7s %-20s %s%n", "ID", "Date", "Hours", "Skill", "Event"));
                for (VolunteerEventAssignment assignment : assignments) {
                    text.append(String.format(Locale.US, "%-4d %-12s %-7s %-20s %s%n",
                        assignment.getId(),
                        assignment.getWorkDate(),
                        formatHours(assignment.getHours()),
                        assignment.getSkillName() != null ? assignment.getSkillName() : "",
                        assignment.getEventTitle() != null
                            ? assignment.getEventTitle()
                            : "Event #" + assignment.getEventId()));
                }
            }

            showTextPreview("Assignments - " + selected.getName(), text.toString());
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void showVolunteerSkills() {
        Volunteer selected = requireSelected();
        if (selected == null) return;

        try {
            List<Skill> skills = volunteerController.getVolunteerSkills(selected.getId());
            StringBuilder text = new StringBuilder();
            text.append("Volunteer: ").append(selected.getName()).append("\n\n");

            if (skills.isEmpty()) {
                text.append("No skills assigned.");
            } else {
                for (Skill skill : skills) {
                    text.append("#").append(skill.getId()).append(" - ").append(skill.getName()).append("\n");
                }
            }

            showTextPreview("Skills - " + selected.getName(), text.toString());
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void addSkillToVolunteer() {
        Volunteer selected = requireSelected();
        if (selected == null) return;

        List<Skill> skills = volunteerController.listSkills();
        if (skills.isEmpty()) {
            appShell.getToast().show(appShell.getStage(),
                "No skill types exist. Add a skill type first.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("skillId", "Skill", FormDialog.Type.COMBO, true, skillOptions(skills))
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Add Skill", fields);
        result.ifPresent(map -> {
            try {
                int skillId = idFromOption(map.get("skillId"));
                volunteerController.addSkill(selected.getId(), skillId);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Skill added.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void removeSkillFromVolunteer() {
        Volunteer selected = requireSelected();
        if (selected == null) return;

        List<Skill> skills = volunteerController.getVolunteerSkills(selected.getId());
        if (skills.isEmpty()) {
            appShell.getToast().show(appShell.getStage(),
                "This volunteer has no skills.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("skillId", "Skill", FormDialog.Type.COMBO, true, skillOptions(skills))
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Remove Skill", fields);
        result.ifPresent(map -> {
            try {
                int skillId = idFromOption(map.get("skillId"));
                volunteerController.removeSkill(selected.getId(), skillId);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Skill removed.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showSearchBySkillDialog() {
        List<Skill> skills = volunteerController.listSkills();
        if (skills.isEmpty()) {
            appShell.getToast().show(appShell.getStage(),
                "No skill types exist. Add a skill type first.", Toast.Type.WARNING);
            return;
        }

        Object[] options = new Object[skills.size() + 1];
        options[0] = "0: All volunteers";
        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            options[i + 1] = skill.getId() + ": " + skill.getName();
        }

        var fields = List.of(
            new FormDialog.Field("skillId", "Skill", FormDialog.Type.COMBO, true, options)
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Search by Skill", fields);
        result.ifPresent(map -> {
            try {
                Object option = map.get("skillId");
                int skillId = idFromOption(option);
                skillFilterId = skillId == 0 ? null : skillId;
                refresh();

                if (skillFilterId == null) {
                    appShell.getToast().show(appShell.getStage(),
                        "Showing all volunteers.", Toast.Type.INFO);
                } else {
                    appShell.getToast().show(appShell.getStage(),
                        "Showing volunteers with skill: " + nameFromOption(option), Toast.Type.INFO);
                }
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showAddSkillTypeDialog() {
        var fields = List.of(
            new FormDialog.Field("name", "Skill Name", FormDialog.Type.TEXT, true)
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Add Skill Type", fields);
        result.ifPresent(map -> {
            try {
                String name = (String) map.get("name");
                Skill skill = volunteerController.createSkill(name);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Skill added: #" + skill.getId() + " - " + skill.getName(), Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showDeleteSkillTypeDialog() {
        List<Skill> skills = volunteerController.listSkills();
        if (skills.isEmpty()) {
            appShell.getToast().show(appShell.getStage(),
                "No skill types exist.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("skillId", "Skill", FormDialog.Type.COMBO, true, skillOptions(skills))
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Delete Skill Type", fields);
        result.ifPresent(map -> {
            try {
                Object option = map.get("skillId");
                int skillId = idFromOption(option);
                String name = nameFromOption(option);

                if (!appShell.getConfirmDialog().ask(appShell.getStage(),
                        "Delete skill type '" + name + "'?")) {
                    return;
                }

                volunteerController.deleteSkill(skillId);
                if (skillFilterId != null && skillFilterId == skillId) {
                    skillFilterId = null;
                }
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Skill #" + skillId + " deleted.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showHoursSummary() {
        Volunteer selected = requireSelected();
        if (selected == null) return;

        try {
            VolunteerSummary summary = volunteerController.summary(selected.getId());
            StringBuilder text = new StringBuilder();
            text.append("Volunteer: ").append(selected.getName()).append("\n");
            text.append("Total: ").append(formatHours(summary.getTotalHours())).append(" hours\n\n");
            text.append("Per event:\n");

            if (summary.getHoursByEvent().isEmpty()) {
                text.append("  (no logged hours)");
            } else {
                for (Map.Entry<String, Double> entry : summary.getHoursByEvent().entrySet()) {
                    text.append("  ").append(entry.getKey()).append(": ")
                        .append(formatHours(entry.getValue())).append("\n");
                }
            }

            showTextPreview("Hours Summary - " + selected.getName(), text.toString());
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void showCertificateDialog() {
        Volunteer selected = requireSelected();
        if (selected == null) return;

        try {
            String cert = volunteerController.certificate(selected.getId());
            showTextPreview("Volunteer Certificate", cert);
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void deleteSelected() {
        Volunteer selected = requireSelected();
        if (selected == null) return;

        if (appShell.getConfirmDialog().ask(appShell.getStage(),
                "Delete volunteer '" + selected.getName() + "'?")) {
            try {
                volunteerController.delete(selected.getId());
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Volunteer deleted.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        }
    }

    private Volunteer requireSelected() {
        Volunteer selected = volunteersTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a volunteer first.", Toast.Type.WARNING);
        }
        return selected;
    }

    private static Object[] availabilityOptions(boolean available) {
        return available ? new Object[]{"Yes", "No"} : new Object[]{"No", "Yes"};
    }

    private static Object[] skillOptions(List<Skill> skills) {
        return skills.stream().map(s -> s.getId() + ": " + s.getName()).toArray();
    }

    private static int idFromOption(Object option) {
        return Integer.parseInt(((String) option).split(":")[0].trim());
    }

    private static String nameFromOption(Object option) {
        String text = (String) option;
        int index = text.indexOf(':');
        return index >= 0 ? text.substring(index + 1).trim() : text;
    }

    private static String formatHours(double hours) {
        return String.format(Locale.US, "%.1f", hours);
    }

    private void showTextPreview(String title, String text) {
        if (appShell != null && appShell.getModalOverlay() != null) {
            ModalOverlay.show(appShell, new ModalOverlay.ModalCard<Void>() {
                private java.util.function.Consumer<Void> closeCallback;

                @Override
                public Node getNode() {
                    VBox card = new VBox();
                    card.getStyleClass().add("modal-card");
                    card.setMaxWidth(620);
                    card.setMinWidth(520);
                    card.setMaxHeight(560);

                    HBox header = new HBox();
                    header.getStyleClass().add("modal-header");
                    header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label titleLabel = new Label(title);
                    titleLabel.getStyleClass().add("modal-title");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button closeBtn = new Button("✕");
                    closeBtn.getStyleClass().add("modal-close-btn");
                    closeBtn.setOnAction(e -> {
                        if (closeCallback != null) closeCallback.accept(null);
                    });

                    header.getChildren().addAll(titleLabel, spacer, closeBtn);

                    VBox body = new VBox(10);
                    body.getStyleClass().add("modal-body");

                    TextArea area = new TextArea(text);
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.getStyleClass().add("preview-area");
                    VBox.setVgrow(area, Priority.ALWAYS);
                    body.getChildren().add(area);
                    VBox.setVgrow(body, Priority.ALWAYS);

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

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(appShell.getStage());
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(new TextArea(text));
        dialog.showAndWait();
    }

    @Override
    public void refresh() {
        List<Volunteer> volunteers = skillFilterId == null
            ? volunteerController.listVolunteers()
            : volunteerController.searchBySkill(skillFilterId);
        volunteersTable.getTableView().setItems(FXCollections.observableArrayList(volunteers));
        volunteersTable.getTableView().refresh();
    }
}
