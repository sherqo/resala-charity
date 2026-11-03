package com.resala.gui.views;

import com.resala.controller.EventController;
import com.resala.event.EventRegistration;
import com.resala.event.EventRequest;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.ModalOverlay;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.Event;
import com.resala.model.EventType;
import com.resala.notification.EventObserver;
import javafx.application.Platform;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EventView extends BaseView implements EventObserver {

    private final AppShell appShell;
    private final EventController eventController;
    private final DataTable<Event> eventsTable;
    private final Map<Integer, String> eventTypeNames;
    private Button primaryActionButton;

    public EventView(AppShell appShell, EventController eventController) {
        this(appShell, eventController, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private EventView(AppShell appShell, EventController eventController, Map<Integer, String> typeMap) {
        super(createToolbar(), DataTable.<Event>builder()
            .column("ID",     60,  (java.util.function.Function<Event, String>) e -> String.valueOf(e.getId()))
            .column("Title",  200, Event::getTitle)
            .column("Type",   120, e -> typeMap.getOrDefault(e.getEventTypeId(), "Type #" + e.getEventTypeId()))
            .column("Date",   120, e -> e.getDate().toString())
            .column("Seats",  100, e -> e.getCapacity() + " spots")
            .column("Hours",  80,  e -> e.getHours() + " hrs")
            .column("Status", 80,  e -> e.getStatus().name())
            .items(FXCollections.observableArrayList())
            .build());

        this.appShell        = appShell;
        this.eventController = eventController;
        this.eventTypeNames = typeMap;
        this.eventsTable = (DataTable<Event>) this.table;

        // Secondary actions
        Button registerBtn = Toolbar.ghost("Register Attendee", e -> showRegisterDialog());
        Button editBtn = Toolbar.ghost("Edit Event", e -> showEditDialog());
        Button openBtn = Toolbar.ghost("Open Event", e -> openSelected());
        Button closeBtn = Toolbar.ghost("Close Event", e -> closeSelected());
        Button reportBtn = Toolbar.ghost("Attendance Report", e -> showAttendanceReport());
        Button remindBtn = Toolbar.ghost("Send Reminder", e -> showRemindDialog());
        Button deleteBtn = Toolbar.ghost("Delete", e -> deleteSelected());
        toolbar.getChildren().addAll(registerBtn, editBtn, openBtn, closeBtn, reportBtn, remindBtn, deleteBtn);

        this.primaryActionButton = Toolbar.primary("Create Event", e -> showCreateEventDialog());
    }

    @Override
    public void onEventUpdated(Event event, String message) {
        Platform.runLater(this::refresh);
    }

    @Override
    public Button getPrimaryAction() { return primaryActionButton; }

    private static HBox createToolbar() { return Toolbar.of(); }

    // -------------------------------------------------------------------------
    // Dialogs & Actions
    // -------------------------------------------------------------------------

    private void showCreateEventDialog() {
        List<EventType> types = eventController.listEventTypes();
        String[] typeOptions = types.isEmpty()
            ? new String[]{"1: General"}
            : types.stream().map(t -> t.getId() + ": " + t.getName()).toArray(String[]::new);

        var fields = List.of(
            new FormDialog.Field("title",       "Title",            FormDialog.Type.TEXT,     true),
            new FormDialog.Field("eventTypeId", "Event Type",       FormDialog.Type.COMBO,    true, (Object[]) typeOptions),
            new FormDialog.Field("date",        "Date (YYYY-MM-DD)",FormDialog.Type.DATE,     true),
            new FormDialog.Field("capacity",    "Capacity",         FormDialog.Type.NUMBER,   true),
            new FormDialog.Field("hours",       "Volunteer Hours",  FormDialog.Type.NUMBER,   true),
            new FormDialog.Field("description", "Description",      FormDialog.Type.TEXTAREA, false)
        );

        Optional<java.util.Map<String, Object>> result =
            FormDialog.show(appShell.getStage(), "Create Event", fields);
        result.ifPresent(map -> {
            try {
                String title        = (String) map.get("title");
                int    eventTypeId  = Integer.parseInt(((String) map.get("eventTypeId")).split(":")[0].trim());
                java.time.LocalDate date = java.time.LocalDate.parse((String) map.get("date"));
                int    capacity     = Integer.parseInt(map.get("capacity").toString());
                int    hours        = Integer.parseInt(map.get("hours").toString());
                String description  = (String) map.get("description");

                eventController.create(new EventRequest(title, eventTypeId, date, capacity, hours, description));
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Event created! (Status: DRAFT — use 'Open Event' to start registrations)", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(),
                    "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showEditDialog() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
            return;
        }

        List<EventType> types = eventController.listEventTypes();
        String currentTypeOption = selected.getEventTypeId() + ": " + eventTypeNames.getOrDefault(selected.getEventTypeId(), "Type #" + selected.getEventTypeId());

        List<String> typeOptionsList = new ArrayList<>();
        typeOptionsList.add(currentTypeOption);
        for (EventType t : types) {
            String opt = t.getId() + ": " + t.getName();
            if (!opt.equals(currentTypeOption)) {
                typeOptionsList.add(opt);
            }
        }
        String[] typeOptions = typeOptionsList.toArray(String[]::new);

        var fields = List.of(
            new FormDialog.Field("title",       "Title",            FormDialog.Type.TEXT,     true, selected.getTitle()),
            new FormDialog.Field("eventTypeId", "Event Type",       FormDialog.Type.COMBO,    true, (Object[]) typeOptions),
            new FormDialog.Field("date",        "Date (YYYY-MM-DD)",FormDialog.Type.DATE,     true, selected.getDate().toString()),
            new FormDialog.Field("capacity",    "Capacity",         FormDialog.Type.NUMBER,   true, String.valueOf(selected.getCapacity())),
            new FormDialog.Field("hours",       "Volunteer Hours",  FormDialog.Type.NUMBER,   true, String.valueOf(selected.getHours())),
            new FormDialog.Field("description", "Description",      FormDialog.Type.TEXTAREA, false, selected.getDescription() != null ? selected.getDescription() : "")
        );

        Optional<java.util.Map<String, Object>> result =
            FormDialog.show(appShell.getStage(), "Edit " + selected.getTitle(), fields);
        result.ifPresent(map -> {
            try {
                String title        = (String) map.get("title");
                int    eventTypeId  = Integer.parseInt(((String) map.get("eventTypeId")).split(":")[0].trim());
                java.time.LocalDate date = java.time.LocalDate.parse((String) map.get("date"));
                int    capacity     = Integer.parseInt(map.get("capacity").toString());
                int    hours        = Integer.parseInt(map.get("hours").toString());
                String description  = (String) map.get("description");

                eventController.update(selected.getId(), new EventRequest(title, eventTypeId, date, capacity, hours, description));
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Event updated!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(),
                    "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void openSelected() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
            return;
        }
        if (selected.getStatus() == Event.Status.OPEN) {
            appShell.getToast().show(appShell.getStage(), "Event is already open.", Toast.Type.INFO);
            return;
        }
        if (selected.getStatus() == Event.Status.CLOSED) {
            appShell.getToast().show(appShell.getStage(),
                "Closed events cannot be reopened.", Toast.Type.WARNING);
            return;
        }
        try {
            eventController.open(selected.getId());
            appShell.refreshAll();
            appShell.getToast().show(appShell.getStage(), "Event \"" + selected.getTitle() + "\" is now OPEN for registration!", Toast.Type.SUCCESS);
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void closeSelected() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
            return;
        }
        if (selected.getStatus() == Event.Status.CLOSED) {
            appShell.getToast().show(appShell.getStage(), "Event is already closed.", Toast.Type.INFO);
            return;
        }
        try {
            eventController.close(selected.getId());
            appShell.refreshAll();
            appShell.getToast().show(appShell.getStage(),
                "Event \"" + selected.getTitle() + "\" is now CLOSED. " + selected.getHours() + " volunteer hour(s) credited to assigned volunteers.",
                Toast.Type.SUCCESS);
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void showRegisterDialog() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
            return;
        }
        if (selected.getStatus() != Event.Status.OPEN) {
            appShell.getToast().show(appShell.getStage(),
                "Event is not open for registration. Use 'Open Event' first.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("name",  "Attendee Name", FormDialog.Type.TEXT, true),
            new FormDialog.Field("email", "Email",         FormDialog.Type.TEXT, false)
        );

        Optional<java.util.Map<String, Object>> result =
            FormDialog.show(appShell.getStage(), "Register for " + selected.getTitle(), fields);
        result.ifPresent(map -> {
            try {
                String name  = (String) map.get("name");
                String email = (String) map.get("email");
                EventRegistration reg = eventController.register(
                    selected.getId(), name, email.isBlank() ? null : email);
                appShell.getToast().show(appShell.getStage(),
                    "Registered! Ticket: " + reg.getTicketRef(), Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(),
                    "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showAttendanceReport() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
            return;
        }
        try {
            String report = eventController.attendanceReport(selected.getId());
            showReportPreview(report);
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void showRescheduleDialog() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
            return;
        }
        try {
            String report = eventController.attendanceReport(selected.getId());
            showReportPreview(report);
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void showReportPreview(String text) {
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

                    Label titleLabel = new Label("Event Attendance Report");
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
        dialog.setTitle("Event Attendance Report");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(new TextArea(text));
        dialog.showAndWait();
    }

    private void showRemindDialog() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
            return;
        }
        List<EventRegistration> regs = eventController.getRegistrations(selected.getId());
        appShell.getToast().show(appShell.getStage(),
            "Reminder sent to " + regs.size() + " attendees for " + selected.getTitle(),
            Toast.Type.INFO);
    }

    private void deleteSelected() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
            return;
        }
        if (appShell.getConfirmDialog().ask(appShell.getStage(),
                "Delete event '" + selected.getTitle() + "'?")) {
            try {
                eventController.delete(selected.getId());
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Event deleted.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(),
                    "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    @Override
    public void refresh() {
        eventTypeNames.clear();
        for (EventType t : eventController.listEventTypes()) {
            eventTypeNames.put(t.getId(), t.getName());
        }

        List<Event> events = eventController.listEvents();
        eventsTable.getTableView().setItems(FXCollections.observableArrayList(events));
        eventsTable.getTableView().refresh();
    }
}
