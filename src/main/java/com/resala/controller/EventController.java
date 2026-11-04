package com.resala.controller;

import com.resala.event.EventRegistration;
import com.resala.event.EventRequest;
import com.resala.event.EventService;
import com.resala.event.EventTypeService;
import com.resala.model.Event;
import com.resala.model.EventType;
import com.resala.volunteer.VolunteerService;
import java.util.List;

public class EventController {

  private final EventService     service;
  private final EventTypeService eventTypeService;
  private final VolunteerService volunteerService;

  public EventController(
      EventService service,
      EventTypeService eventTypeService,
      VolunteerService volunteerService) {
    this.service          = service;
    this.eventTypeService = eventTypeService;
    this.volunteerService = volunteerService;
  }

  public Event create(EventRequest request)              { return service.create(request); }
  public Event update(long id, EventRequest request)     { return service.update(id, request); }
  public void  delete(long id)                           { service.delete(id); }
  public Event open(long id)                             { return service.open(id); }
  public Event close(long id) {
    Event closed = service.close(id);
    return closed;
  }
  public List<Event> listEvents()                        { return service.findAll(); }
  public Event getEvent(long id)                         { return service.getEvent(id); }

  public EventRegistration register(long eventId, String name, String email) {
    return service.register(eventId, name, email);
  }

  public void checkIn(long registrationId) {
    service.checkIn(registrationId);
    EventRegistration reg = service.getRegistration(registrationId);
    if (reg.isVolunteer()) {
      Event event = service.getEvent(reg.getEventId());
      volunteerService.creditVolunteerHours(event, reg.getVolunteerId());
    }
  }
  public String attendanceReport(long eventId)           { return service.attendanceReport(eventId); }

  public void sendReminder(long eventId)                 { service.sendReminder(eventId); }

  public List<EventRegistration> getRegistrations(long eventId) {
    return service.getRegistrations(eventId);
  }

  public List<EventType> listEventTypes()                        { return eventTypeService.findAll(); }
  public EventType       getEventType(int id)                    { return eventTypeService.getById(id); }
  public EventType       createEventType(String name)            { return eventTypeService.create(name); }
  public EventType       renameEventType(int id, String newName) { return eventTypeService.rename(id, newName); }
  public void            deleteEventType(int id)                 { eventTypeService.delete(id); }
}
