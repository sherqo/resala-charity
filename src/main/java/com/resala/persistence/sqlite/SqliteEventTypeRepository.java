package com.resala.persistence.sqlite;

import com.resala.event.EventTypeRepository;
import com.resala.model.EventType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQLite implementation of {@link EventTypeRepository}. */
public class SqliteEventTypeRepository implements EventTypeRepository {

  private final Connection connection;

  public SqliteEventTypeRepository(Connection connection) {
    this.connection = connection;
  }

  @Override
  public List<EventType> findAll() {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT * FROM event_types ORDER BY name");
         ResultSet rs = ps.executeQuery()) {
      List<EventType> list = new ArrayList<>();
      while (rs.next()) list.add(map(rs));
      return list;
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list event types", e);
    }
  }

  @Override
  public Optional<EventType> findById(int id) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT * FROM event_types WHERE id=?")) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(map(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load event type", e);
    }
  }

  @Override
  public Optional<EventType> findByName(String name) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT * FROM event_types WHERE name = ? COLLATE NOCASE")) {
      ps.setString(1, name);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(map(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not search event type by name", e);
    }
  }

  @Override
  public EventType save(EventType eventType) {
    try (PreparedStatement ps = connection.prepareStatement(
        "INSERT INTO event_types (name) VALUES (?)",
        Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, eventType.getName());
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No event_type id returned");
        eventType.setId(keys.getInt(1));
        return eventType;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not save event type", e);
    }
  }

  @Override
  public void rename(int id, String newName) {
    try (PreparedStatement ps = connection.prepareStatement(
        "UPDATE event_types SET name=? WHERE id=?")) {
      ps.setString(1, newName);
      ps.setInt(2, id);
      if (ps.executeUpdate() == 0)
        throw new IllegalArgumentException("Event type not found: #" + id);
    } catch (SQLException e) {
      throw new IllegalStateException("Could not rename event type", e);
    }
  }

  @Override
  public void delete(int id) {
    try (PreparedStatement ps = connection.prepareStatement(
        "DELETE FROM event_types WHERE id=?")) {
      ps.setInt(1, id);
      if (ps.executeUpdate() == 0)
        throw new IllegalArgumentException("Event type not found: #" + id);
    } catch (SQLException e) {
      throw new IllegalStateException("Could not delete event type", e);
    }
  }

  @Override
  public boolean isInUse(int id) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT COUNT(*) FROM events WHERE event_type_id=?")) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not check event type usage", e);
    }
  }

  // Private helper
  private EventType map(ResultSet rs) throws SQLException {
    return new EventType(rs.getInt("id"), rs.getString("name"));
  }
}
