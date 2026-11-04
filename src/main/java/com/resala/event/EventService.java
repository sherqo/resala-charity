package com.resala.event;

import com.resala.model.Event;
import com.resala.notification.EventObserver;
import com.resala.notification.EventSubject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventService implements EventSubject {

  private final EventRepository events;
  private final EventRegistrationRepository registrations;
  private final List<EventObserver> observers = new ArrayList<>();

  public EventService(EventRepository events, EventRegistrationRepository registrations) {
    this.events = events;
    this.registrations = registrations;
  }

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

  public Event create(EventRequest request) {
    validate(request);
    try {
      return events.save(new Event(
        0,
        request.title().trim(),
        request.eventTypeId(),
        request.date(),
        request.capacity(),
        request.hours(),
        Event.Status.DRAFT,
        request.description()
      ));
    } catch (IllegalStateException e) {
      throw new EventException(EventError.PERSISTENCE_FAILURE, null, e);
    }
  }

  public Event update(long id, EventRequest request) {
    Event event = getEvent(id);
    validate(request);

    int registered = registrations.countRegistered(id);
    if (request.capacity() < registered) {
      throw new EventException(
        EventError.CAPACITY_BELOW_REGISTERED,
        "(requested: " + request.capacity() + ", registered: " + registered + ")"
      );
    }

    boolean dateChanged = !event.getDate().equals(request.date());
    event.setTitle(request.title().trim());
    event.setEventTypeId(request.eventTypeId());
    event.setDate(request.date());
    event.setCapacity(request.capacity());
    event.setHours(request.hours());
    event.setDescription(request.description());

    Event saved;
    try {
      saved = events.save(event);
    } catch (IllegalStateException e) {
      throw new EventException(EventError.PERSISTENCE_FAILURE, null, e);
    }

    String message = dateChanged
      ? "Event has been rescheduled. New date: " + saved.getDate()
      : "Event details have been updated.";
    notifyObservers(saved, message);

    return saved;
  }

  public void delete(long id) {
    Event event = getEvent(id);
    if (event.getStatus() != Event.Status.DRAFT) {
      throw new EventException(EventError.DELETE_NON_DRAFT, null);
    }
    try {
      events.delete(id);
    } catch (IllegalStateException e) {
      throw new EventException(EventError.PERSISTENCE_FAILURE, null, e);
    }
  }

  public Event open(long id) {
    Event event = getEvent(id);
    if (event.getStatus() == Event.Status.OPEN) {
      throw new EventException(EventError.EVENT_ALREADY_OPEN, null);
    }
    event.setStatus(Event.Status.OPEN);
    Event saved;
    try {
      saved = events.save(event);
    } catch (IllegalStateException e) {
      throw new EventException(EventError.PERSISTENCE_FAILURE, null, e);
    }
    notifyObservers(saved, "Event registration is now OPEN.");
    return saved;
  }

  public Event close(long id) {
    Event event = getEvent(id);
    if (event.getStatus() != Event.Status.OPEN) {
      throw new EventException(EventError.EVENT_NOT_OPEN, null);
    }
    event.setStatus(Event.Status.CLOSED);
    Event saved;
    try {
      saved = events.save(event);
    } catch (IllegalStateException e) {
      throw new EventException(EventError.PERSISTENCE_FAILURE, null, e);
    }
    notifyObservers(saved, "The event has now been closed for registrations.");
    return saved;
  }

  public EventRegistration register(long eventId, String attendeeName, String attendeeEmail) {
    Event event = getEvent(eventId);
    if (event.getStatus() != Event.Status.OPEN) {
      throw new EventException(EventError.REGISTRATION_CLOSED, null);
    }
    if (attendeeName == null || attendeeName.isBlank()) {
      throw new EventException(EventError.ATTENDEE_NAME_REQUIRED, null);
    }
    int registered = registrations.countRegistered(eventId);
    if (registered >= event.getCapacity()) {
      throw new EventException(
        EventError.EVENT_FULL,
        "(" + event.getCapacity() + " spots)"
      );
    }
    String ticketRef = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    try {
      return registrations.register(new EventRegistration(
        0,
        eventId,
        attendeeName.trim(),
        attendeeEmail == null || attendeeEmail.isBlank() ? null : attendeeEmail.trim(),
        ticketRef,
        false
      ));
    } catch (IllegalStateException e) {
      throw new EventException(EventError.PERSISTENCE_FAILURE, null, e);
    }
  }

  public void checkIn(long registrationId) {
    try {
      registrations.checkIn(registrationId);
    } catch (IllegalArgumentException e) {
      throw new EventException(EventError.REGISTRATION_NOT_FOUND, "#" + registrationId);
    } catch (IllegalStateException e) {
      throw new EventException(EventError.PERSISTENCE_FAILURE, null, e);
    }
  }

  public EventRegistration getRegistration(long id) {
    return registrations.findById(id)
      .orElseThrow(() -> new EventException(EventError.REGISTRATION_NOT_FOUND, "#" + id));
  }

  public List<Event> findAll() {
    try {
      return events.findAll();
    } catch (IllegalStateException e) {
      throw new EventException(EventError.PERSISTENCE_FAILURE, null, e);
    }
  }

  public Event getEvent(long id) {
    try {
      return events.findById(id)
        .orElseThrow(() -> new EventException(EventError.EVENT_NOT_FOUND, "#" + id));
    } catch (EventException e) {
      throw e;
    } catch (IllegalStateException e) {
      throw new EventException(EventError.PERSISTENCE_FAILURE, null, e);
    }
  }

  public List<EventRegistration> getRegistrations(long eventId) {
    try {
      return registrations.findByEvent(eventId);
    } catch (IllegalStateException e) {
      throw new EventException(EventError.PERSISTENCE_FAILURE, null, e);
    }
  }

  public String attendanceReport(long eventId) {
    Event event = getEvent(eventId);
    List<EventRegistration> regs;
    try {
      regs = registrations.findByEvent(eventId);
    } catch (IllegalStateException e) {
      throw new EventException(EventError.PERSISTENCE_FAILURE, null, e);
    }
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

  private static final String LINE = "----------------------------------------";

  private void validate(EventRequest req) {
    if (req.title() == null || req.title().isBlank())
      throw new EventException(EventError.TITLE_REQUIRED, null);
    if (req.date() == null)
      throw new EventException(EventError.DATE_REQUIRED, null);
    if (req.eventTypeId() <= 0)
      throw new EventException(EventError.INVALID_EVENT_TYPE, null);
    if (req.capacity() <= 0)
      throw new EventException(EventError.INVALID_CAPACITY, null);
    if (req.hours() <= 0)
      throw new EventException(EventError.INVALID_HOURS, null);
  }
}
