package com.resala.persistence.sqlite;

import com.resala.event.EventRepository;
import com.resala.model.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteEventRepository implements EventRepository {

  private final Connection connection;

  public SqliteEventRepository(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Event save(Event event) {
    try {
      if (event.getId() == 0) return insert(event);
      try (PreparedStatement ps = connection.prepareStatement(
        """
        UPDATE events
        SET title=?, event_type_id=?, date=?, capacity=?, status=?, description=?
        WHERE id=?
        """
      )) {
        bind(ps, event);
        ps.setLong(7, event.getId());
        ps.executeUpdate();
        return event;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not save event", e);
    }
  }

  @Override
  public Optional<Event> findById(long id) {
    try (PreparedStatement ps = connection.prepareStatement(
      "SELECT * FROM events WHERE id=?"
    )) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(map(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load event", e);
    }
  }

  @Override
  public List<Event> findAll() {
    try (PreparedStatement ps = connection.prepareStatement(
      "SELECT * FROM events ORDER BY date DESC, id DESC"
    )) {
      try (ResultSet rs = ps.executeQuery()) {
        List<Event> list = new ArrayList<>();
        while (rs.next()) list.add(map(rs));
        return list;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list events", e);
    }
  }

  @Override
  public void delete(long id) {
    try (PreparedStatement ps = connection.prepareStatement(
      "DELETE FROM events WHERE id=?"
    )) {
      ps.setLong(1, id);
      if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Event not found");
    } catch (SQLException e) {
      throw new IllegalStateException("Could not delete event", e);
    }
  }

  // Private helpers
  private Event insert(Event event) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement(
      """
      INSERT INTO events (title, event_type_id, date, capacity, status, description)
      VALUES (?, ?, ?, ?, ?, ?)
      """,
      Statement.RETURN_GENERATED_KEYS
    )) {
      bind(ps, event);
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No event id returned");
        event.setId(keys.getLong(1));
        return event;
      }
    }
  }

  private void bind(PreparedStatement ps, Event event) throws SQLException {
    ps.setString(1, event.getTitle());
    ps.setInt(2, event.getEventTypeId());
    ps.setString(3, event.getDate().toString());
    ps.setInt(4, event.getCapacity());
    ps.setString(5, event.getStatus().name());
    ps.setString(6, event.getDescription());
  }

  private Event map(ResultSet rs) throws SQLException {
    return new Event(
      rs.getLong("id"),
      rs.getString("title"),
      rs.getInt("event_type_id"),
      LocalDate.parse(rs.getString("date")),
      rs.getInt("capacity"),
      Event.Status.valueOf(rs.getString("status")),
      rs.getString("description")
    );
  }
}
