package com.resala.event;

import com.resala.model.EventType;
import java.util.List;

public class EventTypeService {

  private final EventTypeRepository repo;

  public EventTypeService(EventTypeRepository repo) {
    this.repo = repo;
  }

  public List<EventType> findAll() {
    return repo.findAll();
  }

  public EventType getById(int id) {
    return repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Event type not found: #" + id));
  }

  public EventType create(String name) {
    String clean = requireName(name);
    checkUnique(clean, -1);
    return repo.save(new EventType(0, clean));
  }

  public EventType rename(int id, String newName) {
    getById(id);
    String clean = requireName(newName);
    checkUnique(clean, id);
    repo.rename(id, clean);
    return repo.findById(id).orElseThrow();
  }

  public void delete(int id) {
    getById(id);
    if (repo.isInUse(id)) {
      throw new IllegalStateException(
          "Cannot delete event type #" + id + ": it is used by one or more events.");
    }
    repo.delete(id);
  }

  private String requireName(String name) {
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("Event type name is required.");
    return name.trim();
  }

  private void checkUnique(String name, int excludeId) {
    repo.findByName(name).ifPresent(existing -> {
      if (existing.getId() != excludeId)
        throw new IllegalArgumentException("Event type already exists: \"" + name + "\"");
    });
  }
}
