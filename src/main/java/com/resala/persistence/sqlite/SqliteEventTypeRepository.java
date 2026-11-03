package com.resala.persistence.sqlite;

import com.resala.core.JdbcRepository;
import com.resala.event.EventTypeRepository;
import com.resala.model.EventType;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** This saves event types in the SQLite database. */
public class SqliteEventTypeRepository extends JdbcRepository implements EventTypeRepository {

  private static final JdbcRepository.RowMapper<EventType> EVENT_TYPE_MAPPER =
    rs -> new EventType((int) lng(rs, "id"), str(rs, "name"));

  public SqliteEventTypeRepository(Connection connection) {
    super(connection);
  }

  @Override
  public List<EventType> findAll() {
    return query("SELECT * FROM event_types ORDER BY name", EVENT_TYPE_MAPPER);
  }

  @Override
  public Optional<EventType> findById(int id) {
    return queryOne("SELECT * FROM event_types WHERE id=?", EVENT_TYPE_MAPPER, id);
  }

  @Override
  public Optional<EventType> findByName(String name) {
    return queryOne("SELECT * FROM event_types WHERE name = ? COLLATE NOCASE", EVENT_TYPE_MAPPER, name);
  }

  @Override
  public EventType save(EventType eventType) {
    long id = insertReturnKey("INSERT INTO event_types (name) VALUES (?)", eventType.getName());
    eventType.setId((int) id);
    return eventType;
  }

  @Override
  public void rename(int id, String newName) {
    int updated = execute("UPDATE event_types SET name=? WHERE id=?", newName, id);
    if (updated == 0) throw new IllegalArgumentException("Event type not found: #" + id);
  }

  @Override
  public void delete(int id) {
    int deleted = execute("DELETE FROM event_types WHERE id=?", id);
    if (deleted == 0) throw new IllegalArgumentException("Event type not found: #" + id);
  }

  @Override
  public boolean isInUse(int id) {
    List<Integer> rows = query("SELECT COUNT(*) FROM events WHERE event_type_id=?",
      rs -> rs.getInt(1), id);
    return !rows.isEmpty() && rows.get(0) > 0;
  }
}
