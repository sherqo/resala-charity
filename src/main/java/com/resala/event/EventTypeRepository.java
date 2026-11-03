package com.resala.event;

import com.resala.model.EventType;
import java.util.List;
import java.util.Optional;

public interface EventTypeRepository {
  /** Returns all event types ordered by name. */
  List<EventType> findAll();

  /** Returns the event type with the given id, or empty if not found. */
  Optional<EventType> findById(int id);

  /** Returns the event type whose name matches (case-insensitive), or empty. */
  Optional<EventType> findByName(String name);

  /** Inserts a new event type and returns it with the generated id set. */
  EventType save(EventType eventType);

  /** Renames an existing event type. */
  void rename(int id, String newName);

  void delete(int id);

  /** Returns true if any event row references this event type. */
  boolean isInUse(int id);
}
