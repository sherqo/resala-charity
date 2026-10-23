package com.resala.persistence.sqlite;

import com.resala.event.EventRegistration;
import com.resala.event.EventRegistrationRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteEventRegistrationRepository implements EventRegistrationRepository {

  private final Connection connection;

  public SqliteEventRegistrationRepository(Connection connection) {
    this.connection = connection;
  }

  @Override
  public EventRegistration register(EventRegistration reg) {
    try (PreparedStatement ps = connection.prepareStatement(
      """
      INSERT INTO event_registrations (event_id, attendee_name, attendee_email, ticket_ref, checked_in)
      VALUES (?, ?, ?, ?, 0)
      """,
      Statement.RETURN_GENERATED_KEYS
    )) {
      ps.setLong(1, reg.getEventId());
      ps.setString(2, reg.getAttendeeName());
      ps.setString(3, reg.getAttendeeEmail());
      ps.setString(4, reg.getTicketRef());
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No registration id returned");
        reg.setId(keys.getLong(1));
        return reg;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not register attendee", e);
    }
  }

  @Override
  public Optional<EventRegistration> findById(long id) {
    try (PreparedStatement ps = connection.prepareStatement(
      "SELECT * FROM event_registrations WHERE id=?"
    )) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(map(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load registration", e);
    }
  }

  @Override
  public List<EventRegistration> findByEvent(long eventId) {
    try (PreparedStatement ps = connection.prepareStatement(
      "SELECT * FROM event_registrations WHERE event_id=? ORDER BY id"
    )) {
      ps.setLong(1, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        List<EventRegistration> list = new ArrayList<>();
        while (rs.next()) list.add(map(rs));
        return list;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list registrations", e);
    }
  }

  @Override
  public void checkIn(long registrationId) {
    try (PreparedStatement ps = connection.prepareStatement(
      "UPDATE event_registrations SET checked_in=1 WHERE id=?"
    )) {
      ps.setLong(1, registrationId);
      if (ps.executeUpdate() == 0)
        throw new IllegalArgumentException("Registration not found: #" + registrationId);
    } catch (SQLException e) {
      throw new IllegalStateException("Could not check in attendee", e);
    }
  }

  @Override
  public int countRegistered(long eventId) {
    try (PreparedStatement ps = connection.prepareStatement(
      "SELECT COUNT(*) FROM event_registrations WHERE event_id=?"
    )) {
      ps.setLong(1, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not count registrations", e);
    }
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private EventRegistration map(ResultSet rs) throws SQLException {
    return new EventRegistration(
      rs.getLong("id"),
      rs.getLong("event_id"),
      rs.getString("attendee_name"),
      rs.getString("attendee_email"),
      rs.getString("ticket_ref"),
      rs.getInt("checked_in") == 1
    );
  }
}
