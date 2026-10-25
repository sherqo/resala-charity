package com.resala.controller;

import com.resala.event.EventRegistration;
import com.resala.event.EventRequest;
import com.resala.event.EventService;
import com.resala.model.Event;
import java.util.List;

/**
 * MVC Controller for the event module.
 * Translates view input into EventService calls.
 * No I/O, no SQL, no role checks with if/else.
 */
public class EventController {

  private final EventService service;

  public EventController(EventService service) {
    this.service = service;
  }

  public Event create(EventRequest request)              { return service.create(request); }
  public Event update(long id, EventRequest request)     { return service.update(id, request); }
  public void  delete(long id)                           { service.delete(id); }
  public Event open(long id)                             { return service.open(id); }
  public Event close(long id)                            { return service.close(id); }
  public List<Event> listEvents()                        { return service.findAll(); }
  public Event getEvent(long id)                         { return service.getEvent(id); }

  public EventRegistration register(long eventId, String name, String email) {
    return service.register(eventId, name, email);
  }

  public void checkIn(long registrationId)               { service.checkIn(registrationId); }
  public String attendanceReport(long eventId)           { return service.attendanceReport(eventId); }

  public List<EventRegistration> getRegistrations(long eventId) {
    return service.getRegistrations(eventId);
  }
}
