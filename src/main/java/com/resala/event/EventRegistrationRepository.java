package com.resala.event;

import java.util.List;
import java.util.Optional;

/** Persistence contract for event registrations. */
public interface EventRegistrationRepository {
  EventRegistration register(EventRegistration registration);
  Optional<EventRegistration> findById(long id);
  List<EventRegistration> findByEvent(long eventId);
  void checkIn(long registrationId);
  int countRegistered(long eventId);
}
