package com.resala.persistence.sqlite;

import com.resala.core.JdbcRepository;
import com.resala.event.EventRepository;
import com.resala.model.Event;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SqliteEventRepository extends JdbcRepository implements EventRepository {

  private static final JdbcRepository.RowMapper<Event> EVENT_MAPPER = rs -> new Event(
    lng(rs, "id"),
    str(rs, "title"),
    num(rs, "event_type_id"),
    LocalDate.parse(str(rs, "date")),
    num(rs, "capacity"),
    Event.Status.valueOf(str(rs, "status")),
    str(rs, "description")
  );

  public SqliteEventRepository(Connection connection) {
    super(connection);
  }

  @Override
  public Event save(Event event) {
    if (event.getId() == 0) {
      long id = insertReturnKey(
        "INSERT INTO events (title, event_type_id, date, capacity, status, description) VALUES (?, ?, ?, ?, ?, ?)",
        event.getTitle(), event.getEventTypeId(), event.getDate().toString(),
        event.getCapacity(), event.getStatus().name(), event.getDescription()
      );
      event.setId(id);
      return event;
    }
    int updated = execute(
      "UPDATE events SET title=?, event_type_id=?, date=?, capacity=?, status=?, description=? WHERE id=?",
      event.getTitle(), event.getEventTypeId(), event.getDate().toString(),
      event.getCapacity(), event.getStatus().name(), event.getDescription(), event.getId()
    );
    if (updated == 0) throw new IllegalArgumentException("Event not found");
    return event;
  }

  @Override
  public Optional<Event> findById(long id) {
    return queryOne("SELECT * FROM events WHERE id=?", EVENT_MAPPER, id);
  }

  @Override
  public List<Event> findAll() {
    return query("SELECT * FROM events ORDER BY date DESC, id DESC", EVENT_MAPPER);
  }

  @Override
  public void delete(long id) {
    int deleted = execute("DELETE FROM events WHERE id=?", id);
    if (deleted == 0) throw new IllegalArgumentException("Event not found");
  }
}
