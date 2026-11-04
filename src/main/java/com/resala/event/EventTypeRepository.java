package com.resala.event;

import com.resala.model.EventType;
import java.util.List;
import java.util.Optional;

public interface EventTypeRepository {

  List<EventType> findAll();

  Optional<EventType> findById(int id);

  Optional<EventType> findByName(String name);

  EventType save(EventType eventType);

  void rename(int id, String newName);

  void delete(int id);

  boolean isInUse(int id);
}
