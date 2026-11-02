package com.resala.gui.views;

import com.resala.controller.EventController;
import com.resala.event.EventRegistration;
import com.resala.event.EventRequest;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.Event;
import com.resala.model.EventType;
import com.resala.notification.EventObserver;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Events view - manage events, registrations, attendance.
 * Implements EventObserver for real-time reactive table updates.
 */
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
            .column("Status", 80,  e -> e.getStatus().name())
            .items(FXCollections.observableArrayList())
            .build());

        this.appShell = appShell;
        this.eventController = eventController;
        this.eventTypeNames = typeMap;
        this.eventsTable = (DataTable<Event>) this.table;

        // Secondary actions
        Button registerBtn = Toolbar.ghost("Register Attendee", e -> showRegisterDialog());
        Button rescheduleBtn = Toolbar.ghost("Reschedule", e -> showRescheduleDialog());
        Button remindBtn = Toolbar.ghost("Send Reminder", e -> showRemindDialog());
        Button deleteBtn = Toolbar.ghost("Delete", e -> deleteSelected());
        toolbar.getChildren().addAll(registerBtn, rescheduleBtn, remindBtn, deleteBtn);

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
    // Dialogs
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
                String description  = (String) map.get("description");

                eventController.create(new EventRequest(title, eventTypeId, date, capacity, description));
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Event created!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(),
                    "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showRegisterDialog() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
            return;
        }
        if (selected.getStatus() != Event.Status.OPEN) {
            appShell.getToast().show(appShell.getStage(),
                "Event is not open for registration.", Toast.Type.WARNING);
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

    private void showRescheduleDialog() {
        Event selected = eventsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select an event first.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("date", "New Date (YYYY-MM-DD)", FormDialog.Type.DATE, true)
        );

        Optional<java.util.Map<String, Object>> result =
            FormDialog.show(appShell.getStage(), "Reschedule " + selected.getTitle(), fields);
        result.ifPresent(map -> {
            try {
                java.time.LocalDate newDate = java.time.LocalDate.parse((String) map.get("date"));
                eventController.update(selected.getId(), new EventRequest(
                    selected.getTitle(), selected.getEventTypeId(), newDate,
                    selected.getCapacity(), selected.getDescription()));
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Event rescheduled!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(),
                    "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
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
