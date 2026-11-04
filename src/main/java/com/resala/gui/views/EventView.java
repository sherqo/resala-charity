package com.resala.gui.views;

import com.resala.controller.EventController;
import com.resala.event.EventRegistration;
import com.resala.event.EventRequest;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.PreviewDialog;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.Event;
import com.resala.model.EventType;
import com.resala.notification.EventObserver;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
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

        Button editBtn = Toolbar.ghost("Edit Event", e -> showEditDialog());
        Button openBtn = Toolbar.ghost("Open Event", e -> openSelected());
        Button closeBtn = Toolbar.ghost("Close Event", e -> closeSelected());
        Button deleteBtn = Toolbar.ghost("Delete", e -> deleteSelected());
        toolbar.getChildren().addAll(editBtn, openBtn, closeBtn, deleteBtn);

        HBox registrationToolbar = Toolbar.of(
            Toolbar.ghost("Register Attendee", e -> showRegisterDialog()),
            Toolbar.ghost("View Registrations", e -> showRegistrations()),
            Toolbar.ghost("Check In Attendee", e -> showCheckInDialog()),
            Toolbar.ghost("Attendance Report", e -> showAttendanceReport()),
            Toolbar.ghost("Send Reminder", e -> showRemindDialog())
        );
        getChildren().add(1, registrationToolbar);

        HBox typesToolbar = Toolbar.of(
            Toolbar.ghost("List Event Types", e -> showEventTypes()),
            Toolbar.ghost("Add Event Type", e -> showAddEventTypeDialog()),
            Toolbar.ghost("Rename Event Type", e -> showRenameEventTypeDialog()),
            Toolbar.ghost("Delete Event Type", e -> showDeleteEventTypeDialog())
        );
        getChildren().add(2, typesToolbar);

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
                "Event \"" + selected.getTitle() + "\" is now CLOSED.",
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

    private void showRegistrations() {
        Event selected = requireSelectedEvent();
        if (selected == null) return;

        try {
            List<EventRegistration> registrations = eventController.getRegistrations(selected.getId());
            StringBuilder text = new StringBuilder();
            text.append("Event: ").append(selected.getTitle()).append("\n");
            text.append("Date: ").append(selected.getDate()).append("\n");
            text.append("Registrations: ").append(registrations.size())
                .append(" / ").append(selected.getCapacity()).append("\n\n");

            if (registrations.isEmpty()) {
                text.append("No registrations found.");
            } else {
                text.append(String.format("%-4s %-10s %-11s %-10s %s%n",
                    "ID", "Ticket", "Checked-in", "Type", "Name"));
                for (EventRegistration registration : registrations) {
                    text.append(String.format("%-4d %-10s %-11s %-10s %s%n",
                        registration.getId(),
                        registration.getTicketRef(),
                        registration.isCheckedIn() ? "Yes" : "No",
                        registration.isVolunteer() ? "Volunteer" : "Attendee",
                        registration.getAttendeeName()));
                }
            }

            showTextPreview("Registrations - " + selected.getTitle(), text.toString());
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void showCheckInDialog() {
        Event selected = requireSelectedEvent();
        if (selected == null) return;

        List<EventRegistration> registrations = eventController.getRegistrations(selected.getId());
        if (registrations.isEmpty()) {
            appShell.getToast().show(appShell.getStage(),
                "No registrations found for this event.", Toast.Type.WARNING);
            return;
        }

        Object[] options = registrations.stream()
            .map(r -> r.getId() + ": " + r.getAttendeeName()
                + " (" + r.getTicketRef() + ")"
                + (r.isCheckedIn() ? " - checked in" : ""))
            .toArray();

        var fields = List.of(
            new FormDialog.Field("registrationId", "Registration", FormDialog.Type.COMBO, true, options)
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Check In Attendee", fields);
        result.ifPresent(map -> {
            try {
                long registrationId = idFromOption(map.get("registrationId"));
                eventController.checkIn(registrationId);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Attendee checked in.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showEventTypes() {
        try {
            List<EventType> types = eventController.listEventTypes();
            StringBuilder text = new StringBuilder();
            text.append("Event Types: ").append(types.size()).append("\n\n");

            if (types.isEmpty()) {
                text.append("No event types found.");
            } else {
                for (EventType type : types) {
                    text.append("#").append(type.getId()).append(" - ").append(type.getName()).append("\n");
                }
            }

            showTextPreview("Event Types", text.toString());
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
    }

    private void showAddEventTypeDialog() {
        var fields = List.of(
            new FormDialog.Field("name", "Event Type Name", FormDialog.Type.TEXT, true)
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Add Event Type", fields);
        result.ifPresent(map -> {
            try {
                String name = (String) map.get("name");
                EventType type = eventController.createEventType(name);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Event type created: #" + type.getId() + " - " + type.getName(), Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showRenameEventTypeDialog() {
        List<EventType> types = eventController.listEventTypes();
        if (types.isEmpty()) {
            appShell.getToast().show(appShell.getStage(),
                "No event types found.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("eventTypeId", "Event Type", FormDialog.Type.COMBO, true, eventTypeOptions(types)),
            new FormDialog.Field("name", "New Name", FormDialog.Type.TEXT, true)
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Rename Event Type", fields);
        result.ifPresent(map -> {
            try {
                int id = idFromOption(map.get("eventTypeId"));
                String newName = (String) map.get("name");
                EventType updated = eventController.renameEventType(id, newName);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Renamed to \"" + updated.getName() + "\".", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showDeleteEventTypeDialog() {
        List<EventType> types = eventController.listEventTypes();
        if (types.isEmpty()) {
            appShell.getToast().show(appShell.getStage(),
                "No event types found.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("eventTypeId", "Event Type", FormDialog.Type.COMBO, true, eventTypeOptions(types))
        );

        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Delete Event Type", fields);
        result.ifPresent(map -> {
            try {
                Object option = map.get("eventTypeId");
                int id = idFromOption(option);
                String name = nameFromOption(option);

                if (!appShell.getConfirmDialog().ask(appShell.getStage(),
                        "Delete event type '" + name + "'?")) {
                    return;
                }

                eventController.deleteEventType(id);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Event type #" + id + " deleted.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
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

    private void showReportPreview(String text) {
        showTextPreview("Event Attendance Report", text);
    }

    private void showTextPreview(String title, String text) {
        PreviewDialog.show(appShell, title, text);
    }

    private void showRemindDialog() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
            return;
        }
        List<EventRegistration> regs = eventController.getRegistrations(selected.getId());
        try {
            eventController.sendReminder(selected.getId());
            appShell.getToast().show(appShell.getStage(),
                "Reminder sent to " + regs.size() + " attendees for " + selected.getTitle(),
                Toast.Type.SUCCESS);
        } catch (Exception ex) {
            appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
        }
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

    private Event requireSelectedEvent() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
        }
        return selected;
    }

    private static Object[] eventTypeOptions(List<EventType> types) {
        return types.stream().map(t -> t.getId() + ": " + t.getName()).toArray();
    }

    private static int idFromOption(Object option) {
        return Integer.parseInt(((String) option).split(":")[0].trim());
    }

    private static String nameFromOption(Object option) {
        String text = (String) option;
        int index = text.indexOf(':');
        return index >= 0 ? text.substring(index + 1).trim() : text;
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
