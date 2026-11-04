package com.resala.persistence.sqlite;

import com.resala.core.JdbcRepository;
import com.resala.model.Skill;
import com.resala.model.Volunteer;
import com.resala.model.VolunteerEventAssignment;
import com.resala.volunteer.VolunteerRepository;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

public class SqliteVolunteerRepository extends JdbcRepository implements VolunteerRepository {

  private static final String ASSIGNMENT_SELECT =
    """
    SELECT vh.*, e.title AS event_title, s.name AS skill_name
    FROM volunteer_hours vh
    JOIN events e ON e.id = vh.event_id
    JOIN skills s ON s.id = vh.skill_id
    """;

  private static final JdbcRepository.RowMapper<Volunteer> VOLUNTEER_MAPPER = rs -> {
    Volunteer volunteer = new Volunteer(
      str(rs, "name"),
      str(rs, "email"),
      str(rs, "phone"),
      num(rs, "available") == 1
    );
    volunteer.setId((int) lng(rs, "id"));
    return volunteer;
  };

  private static final JdbcRepository.RowMapper<Skill> SKILL_MAPPER = rs ->
    new Skill(num(rs, "id"), str(rs, "name"));

  private static final JdbcRepository.RowMapper<VolunteerEventAssignment> ASSIGNMENT_MAPPER = rs -> {
    VolunteerEventAssignment assignment = new VolunteerEventAssignment(
      num(rs, "id"),
      num(rs, "volunteer_id"),
      num(rs, "event_id"),
      num(rs, "skill_id"),
      LocalDate.parse(str(rs, "work_date")),
      (int) rs.getDouble("hours")
    );
    assignment.setEventTitle(str(rs, "event_title"));
    assignment.setSkillName(str(rs, "skill_name"));
    return assignment;
  };

  public SqliteVolunteerRepository(Connection connection) {
    super(connection);
  }

  @Override
  public Volunteer save(Volunteer volunteer) {
    if (volunteer.getId() == 0) {
      try {
        return insertVolunteer(volunteer);
      } catch (java.sql.SQLException e) {
        throw new IllegalStateException("Could not insert volunteer", e);
      }
    }
    int updated = execute(
      "UPDATE volunteers SET name=?, email=?, phone=?, available=? WHERE id=?",
      volunteer.getName(), volunteer.getEmail(), volunteer.getPhone(),
      volunteer.isAvailable() ? 1 : 0, volunteer.getId()
    );
    if (updated == 0) throw new IllegalArgumentException("Volunteer not found");
    return volunteer;
  }

  @Override
  public Volunteer findById(int id) {
    List<Volunteer> rows = query("SELECT * FROM volunteers WHERE id=?", VOLUNTEER_MAPPER, id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  @Override
  public List<Volunteer> findAll() {
    return query("SELECT * FROM volunteers ORDER BY name", VOLUNTEER_MAPPER);
  }

  @Override
  public List<Volunteer> findBySkill(int skillId) {
    return query(
      "SELECT DISTINCT v.* FROM volunteers v JOIN volunteer_skills vs ON vs.volunteer_id = v.id WHERE vs.skill_id = ? ORDER BY v.name",
      VOLUNTEER_MAPPER, skillId);
  }

  @Override
  public void delete(int id) {
    int deleted = execute("DELETE FROM volunteers WHERE id=?", id);
    if (deleted == 0) throw new IllegalArgumentException("Volunteer not found");
  }

  @Override
  public void setAvailability(int id, boolean available) {
    int updated = execute("UPDATE volunteers SET available=? WHERE id=?", available ? 1 : 0, id);
    if (updated == 0) throw new IllegalArgumentException("Volunteer not found");
  }

  @Override
  public List<Skill> findAllSkills() {
    return query("SELECT * FROM skills ORDER BY name", SKILL_MAPPER);
  }

  @Override
  public Skill findSkillById(int skillId) {
    List<Skill> rows = query("SELECT * FROM skills WHERE id=?", SKILL_MAPPER, skillId);
    return rows.isEmpty() ? null : rows.get(0);
  }

  @Override
  public Skill findSkillByName(String name) {
    List<Skill> rows = query("SELECT * FROM skills WHERE name = ? COLLATE NOCASE", SKILL_MAPPER, name);
    return rows.isEmpty() ? null : rows.get(0);
  }

  @Override
  public Skill saveSkill(Skill skill) {
    long id = insertReturnKey("INSERT INTO skills (name) VALUES (?)", skill.getName());
    skill.setId((int) id);
    return skill;
  }

  @Override
  public boolean isSkillInUse(int skillId) {
    List<Integer> rows = query(
      "SELECT (SELECT COUNT(*) FROM volunteer_skills WHERE skill_id = ?) + (SELECT COUNT(*) FROM volunteer_hours WHERE skill_id = ?)",
      rs -> rs.getInt(1), skillId, skillId);
    return !rows.isEmpty() && rows.get(0) > 0;
  }

  @Override
  public void deleteSkill(int skillId) {
    int deleted = execute("DELETE FROM skills WHERE id=?", skillId);
    if (deleted == 0) throw new IllegalArgumentException("Skill not found: #" + skillId);
  }

  @Override
  public List<Skill> findSkillsForVolunteer(int volunteerId) {
    return query(
      "SELECT s.* FROM skills s JOIN volunteer_skills vs ON vs.skill_id = s.id WHERE vs.volunteer_id = ? ORDER BY s.name",
      SKILL_MAPPER, volunteerId);
  }

  @Override
  public void addSkill(int volunteerId, int skillId) {
    execute("INSERT OR IGNORE INTO volunteer_skills (volunteer_id, skill_id) VALUES (?, ?)", volunteerId, skillId);
  }

  @Override
  public void removeSkill(int volunteerId, int skillId) {
    int deleted = execute("DELETE FROM volunteer_skills WHERE volunteer_id=? AND skill_id=?", volunteerId, skillId);
    if (deleted == 0) throw new IllegalArgumentException("Volunteer does not have this skill");
  }

  @Override
  public VolunteerEventAssignment saveAssignment(VolunteerEventAssignment assignment) {
    if (assignment.getId() == 0) {
      try {
        return insertAssignment(assignment);
      } catch (java.sql.SQLException e) {
        throw new IllegalStateException("Could not insert assignment", e);
      }
    }
    int updated = execute(
      "UPDATE volunteer_hours SET skill_id=?, work_date=?, hours=?, logged_at=CURRENT_TIMESTAMP WHERE id=?",
      assignment.getSkillId(), assignment.getWorkDate().toString(), assignment.getHours(), assignment.getId()
    );
    if (updated == 0) throw new IllegalStateException("Could not save volunteer assignment");
    return assignment;
  }

  @Override
  public VolunteerEventAssignment findAssignmentById(int id) {
    List<VolunteerEventAssignment> rows = query(ASSIGNMENT_SELECT + " WHERE vh.id=?", ASSIGNMENT_MAPPER, id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  @Override
  public List<VolunteerEventAssignment> findAssignments(int volunteerId) {
    return query(ASSIGNMENT_SELECT + " WHERE vh.volunteer_id=? ORDER BY vh.work_date DESC, vh.id DESC",
      ASSIGNMENT_MAPPER, volunteerId);
  }

  @Override
  public boolean hasAssignment(int volunteerId, int eventId) {
    List<Integer> rows = query("SELECT 1 FROM volunteer_hours WHERE volunteer_id=? AND event_id=?",
      rs -> rs.getInt(1), volunteerId, eventId);
    return !rows.isEmpty();
  }

  @Override
  public int creditVolunteerHours(int eventId, int volunteerId, int hours) {
    if (hours <= 0) return 0;
    return execute(
      "UPDATE volunteer_hours SET hours=? WHERE event_id=? AND volunteer_id=? AND hours<?",
      hours, eventId, volunteerId, hours
    );
  }

  private Volunteer insertVolunteer(Volunteer volunteer) throws java.sql.SQLException {
    long id = insertReturnKey(
      "INSERT INTO volunteers (name, email, phone, available) VALUES (?, ?, ?, ?)",
      volunteer.getName(), volunteer.getEmail(), volunteer.getPhone(),
      volunteer.isAvailable() ? 1 : 0
    );
    volunteer.setId((int) id);
    return volunteer;
  }

  private VolunteerEventAssignment insertAssignment(VolunteerEventAssignment assignment)
    throws java.sql.SQLException {
    long id = insertReturnKey(
      "INSERT INTO volunteer_hours (volunteer_id, event_id, skill_id, work_date, hours) VALUES (?, ?, ?, ?, ?)",
      assignment.getVolunteerId(), assignment.getEventId(), assignment.getSkillId(),
      assignment.getWorkDate().toString(), assignment.getHours()
    );
    assignment.setId((int) id);
    return assignment;
  }
}
