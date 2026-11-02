package com.resala.view.console;

import com.resala.controller.EventController;
import com.resala.event.EventRegistration;
import com.resala.event.EventRequest;
import com.resala.model.Event;
import com.resala.view.EventView;
import java.time.LocalDate;
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
          case "0" -> running = false;
          default  -> System.out.println("Invalid option.");
        }
      } catch (IllegalArgumentException | IllegalStateException e) {
        printError(e.getMessage());
      }
      System.out.println();
    }
  }

  // Event CRUD
  private void listEvents() {
    List<Event> events = controller.listEvents();
    if (events.isEmpty()) { System.out.println("No events found."); return; }
    System.out.println("ID  | Status  | Date       | Cap  | Title");
    System.out.println(LINE);
    for (Event e : events) {
      System.out.printf("%-3d | %-7s | %-10s | %-4d | %s%n",
        e.getId(), e.getStatus(), e.getDate(), e.getCapacity(), e.getTitle());
    }
  }

  private void createEvent() {
    System.out.println("\n--- Create Event ---");
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

  // Helpers
  private EventRequest readEventRequest(Event current) {
    String title   = value("Title",              current == null ? null : current.getTitle());
    String typeStr = value("Event type ID",      current == null ? null : String.valueOf(current.getEventTypeId()));
    String dateStr = value("Date (YYYY-MM-DD)",  current == null ? null : current.getDate().toString());
    String capStr  = value("Capacity",           current == null ? null : String.valueOf(current.getCapacity()));
    String desc    = value("Description",        current == null ? null : current.getDescription());
    return new EventRequest(
      title,
      Integer.parseInt(typeStr),
      LocalDate.parse(dateStr),
      Integer.parseInt(capStr),
      blankToNull(desc)
    );
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
