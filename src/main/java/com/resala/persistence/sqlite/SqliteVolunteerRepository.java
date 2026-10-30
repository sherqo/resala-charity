package com.resala.persistence.sqlite;

import com.resala.model.Skill;
import com.resala.model.Volunteer;
import com.resala.volunteer.VolunteerAssignment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteVolunteerRepository {

  private static final String ASSIGNMENT_SELECT =
    """
    SELECT vh.*, e.title AS event_title, s.name AS skill_name
    FROM volunteer_hours vh
    JOIN events e ON e.id = vh.event_id
    JOIN skills s ON s.id = vh.skill_id
    """;

  private final Connection connection;

  public SqliteVolunteerRepository(Connection connection) {
    this.connection = connection;
  }

  // ── Volunteers ─────────────────────────────────────────────────────────────
  public Volunteer save(Volunteer volunteer) {
    try {
      if (volunteer.getId() == 0) return insertVolunteer(volunteer);
      try (
        PreparedStatement ps = connection.prepareStatement(
          "UPDATE volunteers SET name=?, email=?, phone=?, available=? WHERE id=?"
        )
      ) {
        bindVolunteer(ps, volunteer);
        ps.setLong(5, volunteer.getId());
        ps.executeUpdate();
        return volunteer;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not save volunteer", e);
    }
  }

  public Optional<Volunteer> findById(long id) {
    try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM volunteers WHERE id=?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapVolunteer(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load volunteer", e);
    }
  }

  public List<Volunteer> findAll() {
    return findVolunteers("SELECT * FROM volunteers ORDER BY name", null);
  }

  public void delete(long id) {
    try (PreparedStatement ps = connection.prepareStatement("DELETE FROM volunteers WHERE id=?")) {
      ps.setLong(1, id);
      if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Volunteer not found");
    } catch (SQLException e) {
      throw new IllegalStateException("Could not delete volunteer", e);
    }
  }

  public void setAvailability(long id, boolean available) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "UPDATE volunteers SET available=? WHERE id=?"
      )
    ) {
      ps.setInt(1, available ? 1 : 0);
      ps.setLong(2, id);
      if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Volunteer not found");
    } catch (SQLException e) {
      throw new IllegalStateException("Could not update availability", e);
    }
  }

  public List<Volunteer> findBySkill(int skillId) {
    return findVolunteers(
      """
      SELECT DISTINCT v.*
      FROM volunteers v
      JOIN volunteer_skills vs ON vs.volunteer_id = v.id
      WHERE vs.skill_id = ?
      ORDER BY v.name
      """,
      skillId
    );
  }

  public List<Volunteer> findAvailable() {
    return findVolunteers("SELECT * FROM volunteers WHERE available=1 ORDER BY name", null);
  }

  // ── Skills lookup ──────────────────────────────────────────────────────────

  public List<Skill> findAllSkills() {
    try (
      PreparedStatement ps = connection.prepareStatement("SELECT * FROM skills ORDER BY name")
    ) {
      try (ResultSet rs = ps.executeQuery()) {
        List<Skill> skills = new ArrayList<>();
        while (rs.next()) skills.add(mapSkill(rs));
        return skills;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list skills", e);
    }
  }

  public Optional<Skill> findSkillById(int skillId) {
    try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM skills WHERE id=?")) {
      ps.setInt(1, skillId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapSkill(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load skill", e);
    }
  }

  public List<Skill> findSkillsForVolunteer(long volunteerId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        """
        SELECT s.*
        FROM skills s
        JOIN volunteer_skills vs ON vs.skill_id = s.id
        WHERE vs.volunteer_id = ?
        ORDER BY s.name
        """
      )
    ) {
      ps.setLong(1, volunteerId);
      try (ResultSet rs = ps.executeQuery()) {
        List<Skill> skills = new ArrayList<>();
        while (rs.next()) skills.add(mapSkill(rs));
        return skills;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list volunteer skills", e);
    }
  }

  public void addSkill(long volunteerId, int skillId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "INSERT OR IGNORE INTO volunteer_skills (volunteer_id, skill_id) VALUES (?, ?)"
      )
    ) {
      ps.setLong(1, volunteerId);
      ps.setInt(2, skillId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Could not add volunteer skill", e);
    }
  }

  public void removeSkill(long volunteerId, int skillId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "DELETE FROM volunteer_skills WHERE volunteer_id=? AND skill_id=?"
      )
    ) {
      ps.setLong(1, volunteerId);
      ps.setInt(2, skillId);
      if (ps.executeUpdate() == 0) {
        throw new IllegalArgumentException("Volunteer does not have this skill");
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not remove volunteer skill", e);
    }
  }

  // ── Assignments and hours ──────────────────────────────────────────────────

  public VolunteerAssignment saveAssignment(VolunteerAssignment assignment) {
    try {
      if (assignment.getId() == 0) return insertAssignment(assignment);
      try (
        PreparedStatement ps = connection.prepareStatement(
          """
          UPDATE volunteer_hours
          SET skill_id=?, work_date=?, hours=?, logged_at=CURRENT_TIMESTAMP
          WHERE id=?
          """
        )
      ) {
        ps.setInt(1, assignment.getSkillId());
        ps.setString(2, assignment.getWorkDate().toString());
        ps.setDouble(3, assignment.getHours());
        ps.setLong(4, assignment.getId());
        ps.executeUpdate();
        return assignment;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not save volunteer assignment", e);
    }
  }

  public Optional<VolunteerAssignment> findAssignmentById(long id) {
    try (
      PreparedStatement ps = connection.prepareStatement(ASSIGNMENT_SELECT + " WHERE vh.id=?")
    ) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(mapAssignment(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load volunteer assignment", e);
    }
  }

  public List<VolunteerAssignment> findAssignments(long volunteerId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        ASSIGNMENT_SELECT + " WHERE vh.volunteer_id=? ORDER BY vh.work_date DESC, vh.id DESC"
      )
    ) {
      ps.setLong(1, volunteerId);
      try (ResultSet rs = ps.executeQuery()) {
        List<VolunteerAssignment> assignments = new ArrayList<>();
        while (rs.next()) assignments.add(mapAssignment(rs));
        return assignments;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list volunteer assignments", e);
    }
  }

  public boolean hasAssignment(long volunteerId, long eventId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "SELECT 1 FROM volunteer_hours WHERE volunteer_id=? AND event_id=?"
      )
    ) {
      ps.setLong(1, volunteerId);
      ps.setLong(2, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not check volunteer assignment", e);
    }
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private Volunteer insertVolunteer(Volunteer volunteer) throws SQLException {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "INSERT INTO volunteers (name, email, phone, available) VALUES (?, ?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS
      )
    ) {
      bindVolunteer(ps, volunteer);
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No volunteer id returned");
        volunteer.setId(keys.getLong(1));
        return volunteer;
      }
    }
  }

  private void bindVolunteer(PreparedStatement ps, Volunteer volunteer) throws SQLException {
    ps.setString(1, volunteer.getName());
    ps.setString(2, volunteer.getEmail());
    ps.setString(3, volunteer.getPhone());
    ps.setInt(4, volunteer.isAvailable() ? 1 : 0);
  }

  private List<Volunteer> findVolunteers(String sql, Integer skillId) {
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      if (skillId != null) ps.setInt(1, skillId);
      try (ResultSet rs = ps.executeQuery()) {
        List<Volunteer> volunteers = new ArrayList<>();
        while (rs.next()) volunteers.add(mapVolunteer(rs));
        return volunteers;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list volunteers", e);
    }
  }

  private VolunteerAssignment insertAssignment(VolunteerAssignment assignment)
    throws SQLException {
    try (
      PreparedStatement ps = connection.prepareStatement(
        """
        INSERT INTO volunteer_hours (volunteer_id, event_id, skill_id, work_date, hours)
        VALUES (?, ?, ?, ?, ?)
        """,
        Statement.RETURN_GENERATED_KEYS
      )
    ) {
      ps.setLong(1, assignment.getVolunteerId());
      ps.setLong(2, assignment.getEventId());
      ps.setInt(3, assignment.getSkillId());
      ps.setString(4, assignment.getWorkDate().toString());
      ps.setDouble(5, assignment.getHours());
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No assignment id returned");
        assignment.setId(keys.getLong(1));
        return assignment;
      }
    }
  }

  private Volunteer mapVolunteer(ResultSet rs) throws SQLException {
    return new Volunteer(
      rs.getLong("id"),
      rs.getString("name"),
      rs.getString("email"),
      rs.getString("phone"),
      rs.getInt("available") == 1
    );
  }

  private Skill mapSkill(ResultSet rs) throws SQLException {
    return new Skill(rs.getInt("id"), rs.getString("name"));
  }

  private VolunteerAssignment mapAssignment(ResultSet rs) throws SQLException {
    VolunteerAssignment assignment = new VolunteerAssignment(
      rs.getLong("id"),
      rs.getLong("volunteer_id"),
      rs.getLong("event_id"),
      rs.getInt("skill_id"),
      LocalDate.parse(rs.getString("work_date")),
      rs.getDouble("hours")
    );
    assignment.setEventTitle(rs.getString("event_title"));
    assignment.setSkillName(rs.getString("skill_name"));
    return assignment;
  }
}
