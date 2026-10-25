package com.resala.event;

/** A single attendee registration for an event. */
public class EventRegistration {

  private long id;
  private long eventId;
  private String attendeeName;
  private String attendeeEmail; // optional
  private String ticketRef;     // unique 8-char code generated on registration
  private boolean checkedIn;

  public EventRegistration() {}

  public EventRegistration(
    long id,
    long eventId,
    String attendeeName,
    String attendeeEmail,
    String ticketRef,
    boolean checkedIn
  ) {
    this.id = id;
    this.eventId = eventId;
    this.attendeeName = attendeeName;
    this.attendeeEmail = attendeeEmail;
    this.ticketRef = ticketRef;
    this.checkedIn = checkedIn;
  }

  public long getId() { return id; }
  public void setId(long id) { this.id = id; }

  public long getEventId() { return eventId; }
  public void setEventId(long eventId) { this.eventId = eventId; }

  public String getAttendeeName() { return attendeeName; }
  public void setAttendeeName(String attendeeName) { this.attendeeName = attendeeName; }

  public String getAttendeeEmail() { return attendeeEmail; }
  public void setAttendeeEmail(String attendeeEmail) { this.attendeeEmail = attendeeEmail; }

  public String getTicketRef() { return ticketRef; }
  public void setTicketRef(String ticketRef) { this.ticketRef = ticketRef; }

  public boolean isCheckedIn() { return checkedIn; }
  public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }
}
