package com.resala.model;

import java.time.LocalDateTime;

public class EventRegistration {
    private long id;
    private long eventId;
    private long volunteerId;
    private String ticketCode;
    private LocalDateTime registeredAt;
    private LocalDateTime attendedAt; // null until check-in

    public EventRegistration() {}

    public EventRegistration(
            long id,
            long eventId,
            long volunteerId,
            String ticketCode,
            LocalDateTime registeredAt,
            LocalDateTime attendedAt
    ) {
        this.id = id;
        this.eventId = eventId;
        this.volunteerId = volunteerId;
        this.ticketCode = ticketCode;
        this.registeredAt = registeredAt;
        this.attendedAt = attendedAt;
    }

    public long getId() {
        return id;
    }

    public long getEventId() {
        return eventId;
    }

    public long getVolunteerId() {
        return volunteerId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public LocalDateTime getAttendedAt() {
        return attendedAt;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }


}
