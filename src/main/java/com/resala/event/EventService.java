package com.resala.event;

import com.resala.model.Event;
import com.resala.notification.EventObserver;
import com.resala.notification.EventSubject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for the event module.
 * Implements EventSubject so that observers (such as EventNotificationObserver)
 * receive notifications when event details change or status transitions occur.
 *
 * Lifecycle rules:
 *   DRAFT → OPEN   (via open)   — allows registrations
 *   OPEN  → CLOSED (via close)  — stops registrations
 *   Events in any status can have their date, title, or capacity updated
 *   (capacity cannot be reduced below already-registered attendee count).
 */
public class EventService implements EventSubject {

  private final EventRepository events;
  private final EventRegistrationRepository registrations;
  private final List<EventObserver> observers = new ArrayList<>();

  public EventService(EventRepository events, EventRegistrationRepository registrations) {
    this.events = events;
    this.registrations = registrations;
  }

  // ── Observer Registration (EventSubject) ──────────────────────────────────

  @Override
  public void addObserver(EventObserver observer) {
    observers.add(observer);
  }

  @Override
  public void removeObserver(EventObserver observer) {
    observers.remove(observer);
  }

  private void notifyObservers(Event event, String message) {
    for (EventObserver observer : observers) {
      observer.onEventUpdated(event, message);
    }
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
   * Updates an event (DRAFT, OPEN, or CLOSED).
   * Supports changing date (e.g. rescheduling) or increasing capacity.
   * Ensures capacity cannot be reduced below current registrations.
   * Registered attendees are notified of any updates via the Observer pattern.
   */
  public Event update(long id, EventRequest request) {
    Event event = getEvent(id);
    validate(request);

    int registered = registrations.countRegistered(id);
    if (request.capacity() < registered) {
      throw new IllegalArgumentException(
        "New capacity (" + request.capacity() + ") cannot be less than existing registrations (" + registered + ")"
      );
    }

    boolean dateChanged = !event.getDate().equals(request.date());
    event.setTitle(request.title().trim());
    event.setEventTypeId(request.eventTypeId());
    event.setDate(request.date());
    event.setCapacity(request.capacity());
    event.setDescription(request.description());
    Event saved = events.save(event);

    String message = dateChanged
      ? "Event has been rescheduled. New date: " + saved.getDate()
      : "Event details have been updated.";
    notifyObservers(saved, message);

    return saved;
  }

  /** Permanently deletes an event. Only allowed while in DRAFT (no registrations). */
  public void delete(long id) {
    Event event = getEvent(id);
    if (event.getStatus() != Event.Status.DRAFT) {
      throw new IllegalStateException("Only DRAFT events with no active history can be deleted");
    }
    events.delete(id);
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  /** DRAFT / CLOSED → OPEN: starts accepting registrations. */
  public Event open(long id) {
    Event event = getEvent(id);
    if (event.getStatus() == Event.Status.OPEN) {
      throw new IllegalStateException("Event is already OPEN");
    }
    event.setStatus(Event.Status.OPEN);
    Event saved = events.save(event);
    notifyObservers(saved, "Event registration is now OPEN.");
    return saved;
  }

  /** OPEN → CLOSED: stops accepting registrations and notifies attendees. */
  public Event close(long id) {
    Event event = getEvent(id);
    if (event.getStatus() != Event.Status.OPEN) {
      throw new IllegalStateException("Only OPEN events can be closed");
    }
    event.setStatus(Event.Status.CLOSED);
    Event saved = events.save(event);

    notifyObservers(saved, "The event has now been closed for registrations.");
    return saved;
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
