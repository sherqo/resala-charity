package com.resala.event;

import com.resala.model.Event;
import java.util.List;
import java.util.Optional;

/** Persistence contract for events. Only SqliteEventRepository implements this. */
public interface EventRepository {
  Event save(Event event);
  Optional<Event> findById(long id);
  List<Event> findAll();
  void delete(long id);
}
