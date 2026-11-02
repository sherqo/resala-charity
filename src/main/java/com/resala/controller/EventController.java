package com.resala.controller;

import com.resala.event.EventRegistration;
import com.resala.event.EventRequest;
import com.resala.event.EventService;
import com.resala.event.EventTypeService;
import com.resala.model.Event;
import com.resala.model.EventType;
import java.util.List;

/**
 * MVC Controller for the event module.
 * Translates view input into EventService / EventTypeService calls.
 * No I/O, no SQL, no role checks with if/else.
 */
public class EventController {

  private final EventService     service;
  private final EventTypeService eventTypeService;

  public EventController(EventService service, EventTypeService eventTypeService) {
    this.service          = service;
    this.eventTypeService = eventTypeService;
  }

  // --- Events ---
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

  // --- Event Types ---
  public List<EventType> listEventTypes()                        { return eventTypeService.findAll(); }
  public EventType       getEventType(int id)                    { return eventTypeService.getById(id); }
  public EventType       createEventType(String name)            { return eventTypeService.create(name); }
  public EventType       renameEventType(int id, String newName) { return eventTypeService.rename(id, newName); }
  public void            deleteEventType(int id)                 { eventTypeService.delete(id); }
}
