package com.resala.view.console;

import com.resala.controller.EventController;
import com.resala.event.EventException;
import com.resala.event.EventRegistration;
import com.resala.event.EventRequest;
import com.resala.model.Event;
import com.resala.model.EventType;
import com.resala.view.EventView;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class ConsoleEventView extends AbstractConsoleView implements EventView {

  private final EventController controller;

  public ConsoleEventView(EventController controller, Scanner input) {
    super(input);
    this.controller = controller;
  }

  @Override
  public void start() {
    boolean running = true;
    while (running) {
      System.out.println(LINE);
      System.out.println("Resala Charity - Event Module");
      System.out.println(LINE);
      System.out.println("1) List all events");
      System.out.println("2) Create event");
      System.out.println("3) Edit event");
      System.out.println("4) Delete event");
      System.out.println("5) Open event (DRAFT / CLOSED → OPEN)");
      System.out.println("6) Close event (OPEN → CLOSED)");
      System.out.println("7) Register attendee");
      System.out.println("8) Check in attendee");
      System.out.println("9) View registrations");
      System.out.println("A) Attendance report");
      System.out.println("B) Manage event types");
      System.out.println("0) Back");
      System.out.println(LINE);
      System.out.print("Choose an option: ");

      try {
        switch (readLine().toUpperCase()) {
          case "1" -> listEvents();
          case "2" -> createEvent();
          case "3" -> editEvent();
          case "4" -> deleteEvent();
          case "5" -> openEvent();
          case "6" -> closeEvent();
          case "7" -> registerAttendee();
          case "8" -> checkIn();
          case "9" -> viewRegistrations();
          case "A" -> attendanceReport();
          case "B" -> manageEventTypes();
          case "0" -> running = false;
          default  -> System.out.println("Invalid option.");
        }
      } catch (EventException e) {
        printError("[" + e.code() + "] " + e.getMessage()
            + (e.retryable() ? " (you may try again)" : ""));
      } catch (NumberFormatException e) {
        printError("Invalid number entered. Please enter a valid integer.");
      }
        catch (IllegalArgumentException | IllegalStateException e) {
        printError(e.getMessage());
      } catch (DateTimeParseException e) {
        printError("Invalid date format. Please use YYYY-MM-DD (e.g. 2026-06-15).");
      }
      System.out.println();
    }
  }

  // =========================================================================
  // Event Types sub-menu
  // =========================================================================
  private void manageEventTypes() {
    boolean running = true;
    while (running) {
      System.out.println(LINE);
      System.out.println("Event Types");
      System.out.println(LINE);
      System.out.println("1) List event types");
      System.out.println("2) Add event type");
      System.out.println("3) Rename event type");
      System.out.println("4) Delete event type");
      System.out.println("0) Back");
      System.out.println(LINE);
      System.out.print("Choose an option: ");

      try {
        switch (readLine().toUpperCase()) {
          case "1" -> listEventTypes();
          case "2" -> addEventType();
          case "3" -> renameEventType();
          case "4" -> deleteEventType();
          case "0" -> running = false;
          default  -> System.out.println("Invalid option.");
        }
      }
      catch (NumberFormatException e) {
        printError("Invalid number entered. Please enter a valid integer.");
      } 
      catch (IllegalArgumentException | IllegalStateException e) {
        printError(e.getMessage());
      } 
      System.out.println();
    }
  }

  private void listEventTypes() {
    List<EventType> types = controller.listEventTypes();
    if (types.isEmpty()) { System.out.println("No event types found."); return; }
    System.out.println("ID  | Name");
    System.out.println(LINE);
    for (EventType t : types) {
      System.out.printf("%-3d | %s%n", t.getId(), t.getName());
    }
  }

  private void addEventType() {
    System.out.println("\n--- Add Event Type ---");
    String name = requiredLine("Name: ");
    EventType created = controller.createEventType(name);
    printSuccess("Event type created: #" + created.getId() + " — " + created.getName());
  }

  private void renameEventType() {
    System.out.println("\n--- Rename Event Type ---");
    listEventTypes();
    int id = readInt("Event type ID to rename: ");
    EventType current = controller.getEventType(id);
    System.out.println("Renaming: \"" + current.getName() + "\"");
    String newName = requiredLine("New name: ");
    EventType updated = controller.renameEventType(id, newName);
    printSuccess("Renamed to \"" + updated.getName() + "\".");
  }

  private void deleteEventType() {
    System.out.println("\n--- Delete Event Type ---");
    listEventTypes();
    int id = readInt("Event type ID to delete: ");
    EventType type = controller.getEventType(id);
    if (readLine("Delete \"" + type.getName() + "\"? (yes/no): ").equalsIgnoreCase("yes")) {
      controller.deleteEventType(id);
      printSuccess("Event type #" + id + " deleted.");
    } else {
      System.out.println("Cancelled.");
    }
  }

  // =========================================================================
  // Events
  // =========================================================================
  private void listEvents() {
    List<EventType> types = controller.listEventTypes();
    List<Event> events = controller.listEvents();
    if (events.isEmpty()) { System.out.println("No events found."); return; }
    System.out.println("ID  | Status  | Date       | Cap  | Type             | Title");
    System.out.println(LINE);
    for (Event e : events) {
      String typeName = types.stream()
          .filter(t -> t.getId() == e.getEventTypeId())
          .map(EventType::getName)
          .findFirst()
          .orElse("Type #" + e.getEventTypeId());
      System.out.printf("%-3d | %-7s | %-10s | %-4d | %-16s | %s%n",
        e.getId(), e.getStatus(), e.getDate(), e.getCapacity(), typeName, e.getTitle());
    }
  }

  private void createEvent() {
    System.out.println("\n--- Create Event ---");
    listEventTypes();
    EventRequest request = readEventRequest(null);
    Event event = controller.create(request);
    printSuccess("Event created: #" + event.getId() + " — " + event.getTitle() + " [DRAFT]");
  }

  private void editEvent() {
    System.out.println("\n--- Edit Event ---");
    long id = readLong("Event ID to edit: ");
    Event current = controller.getEvent(id);
    System.out.println("Editing: #" + current.getId() + " — " + current.getTitle()
      + " | " + current.getDate() + " | cap: " + current.getCapacity());
    System.out.println("Press Enter to keep current value.");
    listEventTypes();
    Event updated = controller.update(id, readEventRequest(current));
    printSuccess("Event #" + updated.getId() + " updated.");
  }

  private void deleteEvent() {
    System.out.println("\n--- Delete Event (DRAFT only) ---");
    long id = readLong("Event ID to delete: ");
    Event event = controller.getEvent(id);
    if (readLine("Delete \"" + event.getTitle() + "\"? (yes/no): ").equalsIgnoreCase("yes")) {
      controller.delete(id);
      printSuccess("Event #" + id + " deleted.");
    } else {
      System.out.println("Cancelled.");
    }
  }

  // Lifecycle
  private void openEvent() {
    System.out.println("\n--- Open Event ---");
    long id = readLong("Event ID to open: ");
    Event event = controller.open(id);
    printSuccess("Event \"" + event.getTitle() + "\" is now OPEN for registration.");
  }

  private void closeEvent() {
    System.out.println("\n--- Close Event ---");
    long id = readLong("Event ID to close: ");
    Event event = controller.close(id);
    printSuccess("Event \"" + event.getTitle() + "\" is now CLOSED.");
  }

  // Registration
  private void registerAttendee() {
    System.out.println("\n--- Register Attendee ---");
    listEvents();
    long eventId = readLong("Event ID: ");
    String name  = requiredLine("Attendee name: ");
    String email = readLine("Attendee email (optional): ");
    EventRegistration reg = controller.register(eventId, name, email);
    printSuccess("Registered! Ticket ref: " + reg.getTicketRef());
  }

  private void checkIn() {
    System.out.println("\n--- Check In Attendee ---");
    long eventId = readLong("Event ID: ");
    viewRegistrationsFor(eventId);
    long regId = readLong("Registration ID to check in: ");
    controller.checkIn(regId);
    printSuccess("Attendee checked in.");
  }

  private void viewRegistrations() {
    System.out.println("\n--- View Registrations ---");
    long eventId = readLong("Event ID: ");
    viewRegistrationsFor(eventId);
  }

  private void attendanceReport() {
    System.out.println("\n--- Attendance Report ---");
    long eventId = readLong("Event ID: ");
    System.out.println("\n" + controller.attendanceReport(eventId));
  }

  // =========================================================================
  // Helpers
  // =========================================================================
  private EventRequest readEventRequest(Event current) {
    String title   = value("Title",              current == null ? null : current.getTitle());
    String typeStr = value("Event type ID",      current == null ? null : String.valueOf(current.getEventTypeId()));
    String dateStr = value("Date (YYYY-MM-DD)",  current == null ? null : current.getDate().toString());
    String capStr  = value("Capacity",           current == null ? null : String.valueOf(current.getCapacity()));
    String desc    = value("Description",        current == null ? null : current.getDescription());

    try {
      return new EventRequest(
        title,
        Integer.parseInt(typeStr),
        LocalDate.parse(dateStr),
        Integer.parseInt(capStr),
        blankToNull(desc)
      );
    } catch (NumberFormatException e) {
      throw new NumberFormatException("Event type ID and Capacity must be valid integers.");
    } catch (DateTimeParseException e) {
      throw new DateTimeParseException(
        "Date must be in YYYY-MM-DD format (e.g. 2026-06-15).",
        e.getParsedString(), e.getErrorIndex());
    }
  }

  private void viewRegistrationsFor(long eventId) {
    List<EventRegistration> regs = controller.getRegistrations(eventId);
    if (regs.isEmpty()) { System.out.println("No registrations found."); return; }
    System.out.println("ID  | Ticket   | Checked-in | Name");
    System.out.println(LINE);
    for (EventRegistration r : regs) {
      System.out.printf("%-3d | %-8s | %-10s | %s%n",
        r.getId(), r.getTicketRef(), r.isCheckedIn() ? "Yes" : "No", r.getAttendeeName());
    }
  }
}
