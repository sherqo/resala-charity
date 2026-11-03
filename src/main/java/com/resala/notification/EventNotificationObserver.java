package com.resala.notification;

import com.resala.core.AuditLogManager;
import com.resala.event.EventRegistration;
import com.resala.event.EventRegistrationRepository;
import com.resala.model.Event;
import java.util.List;
import java.util.Objects;

public class EventNotificationObserver implements EventObserver {

  private final EventRegistrationRepository registrationRepository;
  private final Notifier notifier;

  public EventNotificationObserver(
    EventRegistrationRepository registrationRepository,
    Notifier notifier
  ) {
    this.registrationRepository = Objects.requireNonNull(registrationRepository, "Registration repository must not be null");
    this.notifier = Objects.requireNonNull(notifier, "Notifier must not be null");
  }

  @Override
  public void onEventUpdated(Event event, String message) {
    List<EventRegistration> attendees = registrationRepository.findByEvent(event.getId());

    AuditLogManager.getInstance().log(
        "Observer",
        "Events",
        "Observer Triggers",
        attendees.size() + " attendee(s) / volunteer(s)",
        "Event broadcast: " + event.getTitle(),
        "Observer Pattern triggered via EventSubject for event #" + event.getId() + " (" + event.getTitle() + ").\n"
        + "Status: " + event.getStatus() + "\nDate: " + event.getDate() + "\nMessage: " + message
        + "\nNotifying " + attendees.size() + " registered attendee(s) and volunteer(s)."
    );

    for (EventRegistration attendee : attendees) {
      if (attendee.getAttendeeEmail() != null && !attendee.getAttendeeEmail().isBlank()) {
        String subject = "Update regarding event: " + event.getTitle();
        String body = "Hello " + attendee.getAttendeeName() + ",\n" +
                      message + "\n" +
                      "Event Date: " + event.getDate() + " (Ticket: " + attendee.getTicketRef() + ")";
        notifier.send(attendee.getAttendeeEmail(), subject, body);
      }
    }
  }
}
