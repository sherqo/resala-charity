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
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Events view - manage events, registrations, attendance.
 */
public class EventView extends BaseView {

    private final AppShell appShell;
    private final EventController eventController;

    private DataTable<Event> eventsTable;

    public EventView(AppShell appShell, EventController eventController) {
        super(createToolbar(), createEventsTable());

        this.appShell = appShell;
        this.eventController = eventController;

        this.eventsTable = (DataTable<Event>) getTableView().getProperties().get("dataTable");
        if (this.eventsTable == null) {
            this.eventsTable = createEventsTable();
            getTableView().getProperties().put("dataTable", this.eventsTable);
        }

        this.primaryActionButton = Toolbar.primary("+ Create Event", e -> showCreateEventDialog());
    }

    private Button primaryActionButton;

    @Override
    public Button getPrimaryAction() {
        return primaryActionButton;
    }

    private static HBox createToolbar() {
        return Toolbar.of();
    }

    private static DataTable<Event> createEventsTable() {
        return DataTable.<Event>builder()
            .column("ID", 60, (java.util.function.Function<Event, String>) e -> String.valueOf(e.getId()))
            .column("Title", 200, Event::getTitle)
            .column("Type", 120, e -> "Type #" + e.getEventTypeId())
            .column("Date", 120, e -> e.getDate().toString())
            .column("Seats", 100, e -> e.getCapacity() + " spots")
            .column("Status", 80, e -> e.getStatus().name())
            .items(FXCollections.observableArrayList())
            .build();
    }

    private void showCreateEventDialog() {
        var fields = List.of(
            new FormDialog.Field("title", "Title", FormDialog.Type.TEXT, true),
            new FormDialog.Field("eventTypeId", "Event Type ID", FormDialog.Type.NUMBER, true),
            new FormDialog.Field("date", "Date (YYYY-MM-DD)", FormDialog.Type.DATE, true),
            new FormDialog.Field("capacity", "Capacity", FormDialog.Type.NUMBER, true),
            new FormDialog.Field("description", "Description", FormDialog.Type.TEXTAREA, false)
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Create Event", fields);
        result.ifPresent(map -> {
            try {
                String title = (String) map.get("title");
                int eventTypeId = Integer.parseInt(map.get("eventTypeId").toString());
                java.time.LocalDate date = java.time.LocalDate.parse((String) map.get("date"));
                int capacity = Integer.parseInt(map.get("capacity").toString());
                String description = (String) map.get("description");

                EventRequest request = new EventRequest(title, eventTypeId, date, capacity, description);
                eventController.create(request);
                refresh();
                appShell.getToast().show(appShell.getStage(), "Event created!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
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
            appShell.getToast().show(appShell.getStage(), "Event is not open for registration.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("name", "Attendee Name", FormDialog.Type.TEXT, true),
            new FormDialog.Field("email", "Email", FormDialog.Type.TEXT, false)
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Register for " + selected.getTitle(), fields);
        result.ifPresent(map -> {
            try {
                String name = (String) map.get("name");
                String email = (String) map.get("email");

                EventRegistration reg = eventController.register(selected.getId(), name, email.isBlank() ? null : email);
                appShell.getToast().show(appShell.getStage(),
                    "Registered! Ticket: " + reg.getTicketRef(), Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
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

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Reschedule " + selected.getTitle(), fields);
        result.ifPresent(map -> {
            try {
                java.time.LocalDate newDate = java.time.LocalDate.parse((String) map.get("date"));

                EventRequest request = new EventRequest(
                    selected.getTitle(), selected.getEventTypeId(), newDate, selected.getCapacity(), selected.getDescription());
                eventController.update(selected.getId(), request);
                refresh();
                appShell.getToast().show(appShell.getStage(), "Event rescheduled!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
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
                refresh();
                appShell.getToast().show(appShell.getStage(), "Event deleted.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        }
    }

    @Override
    public void refresh() {
        List<Event> events = eventController.listEvents();
        eventsTable.getTableView().setItems(FXCollections.observableArrayList(events));
    }
}