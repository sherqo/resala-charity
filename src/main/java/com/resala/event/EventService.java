package com.resala.event;

import com.resala.model.Event;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for the event module.
 *
 * Lifecycle rules:
 *   DRAFT → OPEN   (via open)   — allows registrations
 *   OPEN  → CLOSED (via close)  — stops registrations
 *   Only DRAFT events can be edited or deleted.
 *   Only OPEN events accept registrations.
 */
public class EventService {

  private final EventRepository events;
  private final EventRegistrationRepository registrations;

  public EventService(EventRepository events, EventRegistrationRepository registrations) {
    this.events = events;
    this.registrations = registrations;
  }

  // ── CRUD ──────────────────────────────────────────────────────────────────

  /** Creates a new event in DRAFT status. */
  public Event create(EventRequest request) {
    validate(request);
    return events.save(new Event(
      0,
      request.title().trim(),
      request.eventTypeId(),
      request.date(),
      request.capacity(),
      Event.Status.DRAFT,
      request.description()
    ));
  }

  /**
   * Updates a DRAFT event.
   * Editing an OPEN or CLOSED event is rejected to protect existing registrations.
   */
  public Event update(long id, EventRequest request) {
    Event event = getEvent(id);
    if (event.getStatus() != Event.Status.DRAFT) {
      throw new IllegalStateException("Only DRAFT events can be edited");
    }
    validate(request);
    event.setTitle(request.title().trim());
    event.setEventTypeId(request.eventTypeId());
    event.setDate(request.date());
    event.setCapacity(request.capacity());
    event.setDescription(request.description());
    return events.save(event);
  }

  /** Permanently deletes a DRAFT event. */
  public void delete(long id) {
    Event event = getEvent(id);
    if (event.getStatus() != Event.Status.DRAFT) {
      throw new IllegalStateException("Only DRAFT events can be deleted");
    }
    events.delete(id);
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  /** DRAFT → OPEN: starts accepting registrations. */
  public Event open(long id) {
    Event event = getEvent(id);
    if (event.getStatus() != Event.Status.DRAFT) {
      throw new IllegalStateException("Only DRAFT events can be opened");
    }
    event.setStatus(Event.Status.OPEN);
    return events.save(event);
  }

  /** OPEN → CLOSED: stops accepting registrations. */
  public Event close(long id) {
    Event event = getEvent(id);
    if (event.getStatus() != Event.Status.OPEN) {
      throw new IllegalStateException("Only OPEN events can be closed");
    }
    event.setStatus(Event.Status.CLOSED);
    return events.save(event);
  }

  // ── Registration ───────────────────────────────────────────────────────────

  /**
   * Registers an attendee for an OPEN event.
   * Enforces capacity limit and generates a unique ticket reference (E4).
   */
  public EventRegistration register(long eventId, String attendeeName, String attendeeEmail) {
    Event event = getEvent(eventId);
    if (event.getStatus() != Event.Status.OPEN) {
      throw new IllegalStateException("Registrations are only accepted for OPEN events");
    }
    if (attendeeName == null || attendeeName.isBlank()) {
      throw new IllegalArgumentException("Attendee name is required");
    }
    int registered = registrations.countRegistered(eventId);
    if (registered >= event.getCapacity()) {
      throw new IllegalStateException(
        "Event is at full capacity (" + event.getCapacity() + " spots)"
      );
    }
    String ticketRef = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    return registrations.register(new EventRegistration(
      0,
      eventId,
      attendeeName.trim(),
      attendeeEmail == null || attendeeEmail.isBlank() ? null : attendeeEmail.trim(),
      ticketRef,
      false
    ));
  }

  /** Marks a registration as checked-in (E5). */
  public void checkIn(long registrationId) {
    registrations.checkIn(registrationId);
  }

  // ── Queries ────────────────────────────────────────────────────────────────

  public List<Event> findAll() {
    return events.findAll();
  }

  public Event getEvent(long id) {
    return events.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Event not found: #" + id));
  }

  public List<EventRegistration> getRegistrations(long eventId) {
    return registrations.findByEvent(eventId);
  }

  /**
   * Produces a plain-text attendance report (E7):
   * registered / attended / no-show counts.
   */
  public String attendanceReport(long eventId) {
    Event event = getEvent(eventId);
    List<EventRegistration> regs = registrations.findByEvent(eventId);
    long attended = regs.stream().filter(EventRegistration::isCheckedIn).count();
    long noShow   = regs.size() - attended;
    return String.join("\n",
      "RESALA CHARITY — Attendance Report",
      LINE,
      "Event    : " + event.getTitle(),
      "Date     : " + event.getDate(),
      "Status   : " + event.getStatus(),
      LINE,
      "Registered : " + regs.size() + " / " + event.getCapacity(),
      "Attended   : " + attended,
      "No-show    : " + noShow
    );
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private static final String LINE = "----------------------------------------";

  private void validate(EventRequest req) {
    if (req.title() == null || req.title().isBlank())
      throw new IllegalArgumentException("Event title is required");
    if (req.date() == null)
      throw new IllegalArgumentException("Event date is required");
    if (req.eventTypeId() <= 0)
      throw new IllegalArgumentException("A valid event type is required");
    if (req.capacity() <= 0)
      throw new IllegalArgumentException("Capacity must be a positive number");
  }
}
