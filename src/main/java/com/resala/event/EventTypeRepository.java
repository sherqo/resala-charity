package com.resala.event;

import com.resala.model.EventType;
import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for the event_types lookup table.
 * Only {@link com.resala.persistence.sqlite.SqliteEventTypeRepository} implements this.
 */
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

  /**
   * Deletes an event type by id.
   * The caller is responsible for ensuring no events reference this type first.
   */
  void delete(int id);

  /** Returns true if any event row references this event type. */
  boolean isInUse(int id);
}
