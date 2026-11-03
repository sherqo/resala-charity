package com.resala.persistence.sqlite;

import com.resala.core.JdbcRepository;
import com.resala.event.EventRegistration;
import com.resala.event.EventRegistrationRepository;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class SqliteEventRegistrationRepository extends JdbcRepository implements EventRegistrationRepository {

  private static final JdbcRepository.RowMapper<EventRegistration> REG_MAPPER = rs -> {
    EventRegistration reg = new EventRegistration(
      lng(rs, "id"),
      lng(rs, "event_id"),
      str(rs, "attendee_name"),
      str(rs, "attendee_email"),
      str(rs, "ticket_ref"),
      num(rs, "checked_in") == 1
    );
    int volunteerId = num(rs, "volunteer_id");
    if (!rs.wasNull()) reg.setVolunteerId(volunteerId);
    int skillId = num(rs, "skill_id");
    if (!rs.wasNull()) reg.setSkillId(skillId);
    return reg;
  };

  public SqliteEventRegistrationRepository(Connection connection) {
    super(connection);
  }

  @Override
  public EventRegistration register(EventRegistration reg) {
    long id = insertReturnKey(
      "INSERT INTO event_registrations (event_id, attendee_name, attendee_email, ticket_ref, checked_in, volunteer_id, skill_id) VALUES (?, ?, ?, ?, 0, ?, ?)",
      reg.getEventId(), reg.getAttendeeName(), reg.getAttendeeEmail(), reg.getTicketRef(),
      reg.getVolunteerId(), reg.getSkillId()
    );
    reg.setId(id);
    return reg;
  }

  @Override
  public Optional<EventRegistration> findById(long id) {
    return queryOne("SELECT * FROM event_registrations WHERE id=?", REG_MAPPER, id);
  }

  @Override
  public List<EventRegistration> findByEvent(long eventId) {
    return query("SELECT * FROM event_registrations WHERE event_id=? ORDER BY id", REG_MAPPER, eventId);
  }

  @Override
  public void checkIn(long registrationId) {
    int updated = execute("UPDATE event_registrations SET checked_in=1 WHERE id=?", registrationId);
    if (updated == 0) throw new IllegalArgumentException("Registration not found: #" + registrationId);
  }

  @Override
  public int countRegistered(long eventId) {
    List<Integer> rows = query("SELECT COUNT(*) FROM event_registrations WHERE event_id=?",
      rs -> rs.getInt(1), eventId);
    return rows.isEmpty() ? 0 : rows.get(0);
  }

  @Override
  public Optional<EventRegistration> findByVolunteerAndEvent(long volunteerId, long eventId) {
    return queryOne("SELECT * FROM event_registrations WHERE volunteer_id=? AND event_id=?", REG_MAPPER, volunteerId, eventId);
  }

  @Override
  public void deleteByVolunteerAndEvent(long volunteerId, long eventId) {
    execute("DELETE FROM event_registrations WHERE volunteer_id=? AND event_id=?", volunteerId, eventId);
  }
}
