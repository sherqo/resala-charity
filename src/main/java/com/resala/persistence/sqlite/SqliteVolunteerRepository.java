package com.resala.persistence.sqlite;

import com.resala.model.Skill;
import com.resala.model.Volunteer;
import com.resala.model.VolunteerEventAssignment;
import com.resala.volunteer.VolunteerRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SqliteVolunteerRepository implements VolunteerRepository {

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

  @Override
  public Volunteer save(Volunteer volunteer) {
    try {
      if (volunteer.getId() == 0) return insertVolunteer(volunteer);
      try (
        PreparedStatement ps = connection.prepareStatement(
          "UPDATE volunteers SET name=?, email=?, phone=?, available=? WHERE id=?"
        )
      ) {
        bindVolunteer(ps, volunteer);
        ps.setInt(5, volunteer.getId());
        ps.executeUpdate();
        return volunteer;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not save volunteer", e);
    }
  }

  @Override
  public Volunteer findById(int id) {
    try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM volunteers WHERE id=?")) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? mapVolunteer(rs) : null;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load volunteer", e);
    }
  }

  @Override
  public List<Volunteer> findAll() {
    return findVolunteers("SELECT * FROM volunteers ORDER BY name");
  }

  @Override
  public List<Volunteer> findAvailable() {
    return findVolunteers("SELECT * FROM volunteers WHERE available=1 ORDER BY name");
  }

  @Override
  public List<Volunteer> findBySkill(int skillId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        """
        SELECT DISTINCT v.*
        FROM volunteers v
        JOIN volunteer_skills vs ON vs.volunteer_id = v.id
        WHERE vs.skill_id = ?
        ORDER BY v.name
        """
      )
    ) {
      ps.setInt(1, skillId);
      try (ResultSet rs = ps.executeQuery()) {
        return readVolunteers(rs);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not search volunteers", e);
    }
  }

  @Override
  public void delete(int id) {
    try (PreparedStatement ps = connection.prepareStatement("DELETE FROM volunteers WHERE id=?")) {
      ps.setInt(1, id);
      if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Volunteer not found");
    } catch (SQLException e) {
      throw new IllegalStateException("Could not delete volunteer", e);
    }
  }

  @Override
  public void setAvailability(int id, boolean available) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "UPDATE volunteers SET available=? WHERE id=?"
      )
    ) {
      ps.setInt(1, available ? 1 : 0);
      ps.setInt(2, id);
      if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Volunteer not found");
    } catch (SQLException e) {
      throw new IllegalStateException("Could not update availability", e);
    }
  }

  @Override
  public List<Skill> findAllSkills() {
    try (
      PreparedStatement ps = connection.prepareStatement("SELECT * FROM skills ORDER BY name");
      ResultSet rs = ps.executeQuery()
    ) {
      return readSkills(rs);
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list skills", e);
    }
  }

  @Override
  public Skill findSkillById(int skillId) {
    try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM skills WHERE id=?")) {
      ps.setInt(1, skillId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? mapSkill(rs) : null;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load skill", e);
    }
  }

  @Override
  public Skill findSkillByName(String name) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "SELECT * FROM skills WHERE name = ? COLLATE NOCASE"
      )
    ) {
      ps.setString(1, name);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? mapSkill(rs) : null;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load skill", e);
    }
  }

  @Override
  public Skill saveSkill(Skill skill) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "INSERT INTO skills (name) VALUES (?)",
        Statement.RETURN_GENERATED_KEYS
      )
    ) {
      ps.setString(1, skill.getName());
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No skill id returned");
        skill.setId(keys.getInt(1));
        return skill;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not add skill", e);
    }
  }

  @Override
  public boolean isSkillInUse(int skillId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        """
        SELECT
          (SELECT COUNT(*) FROM volunteer_skills WHERE skill_id = ?) +
          (SELECT COUNT(*) FROM volunteer_hours WHERE skill_id = ?)
        """
      )
    ) {
      ps.setInt(1, skillId);
      ps.setInt(2, skillId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not check skill usage", e);
    }
  }

  @Override
  public void deleteSkill(int skillId) {
    try (PreparedStatement ps = connection.prepareStatement("DELETE FROM skills WHERE id=?")) {
      ps.setInt(1, skillId);
      if (ps.executeUpdate() == 0) {
        throw new IllegalArgumentException("Skill not found: #" + skillId);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not delete skill", e);
    }
  }

  @Override
  public List<Skill> findSkillsForVolunteer(int volunteerId) {
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
      ps.setInt(1, volunteerId);
      try (ResultSet rs = ps.executeQuery()) {
        return readSkills(rs);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list volunteer skills", e);
    }
  }

  @Override
  public void addSkill(int volunteerId, int skillId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "INSERT OR IGNORE INTO volunteer_skills (volunteer_id, skill_id) VALUES (?, ?)"
      )
    ) {
      ps.setInt(1, volunteerId);
      ps.setInt(2, skillId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Could not add volunteer skill", e);
    }
  }

  @Override
  public void removeSkill(int volunteerId, int skillId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "DELETE FROM volunteer_skills WHERE volunteer_id=? AND skill_id=?"
      )
    ) {
      ps.setInt(1, volunteerId);
      ps.setInt(2, skillId);
      if (ps.executeUpdate() == 0) {
        throw new IllegalArgumentException("Volunteer does not have this skill");
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not remove volunteer skill", e);
    }
  }

  @Override
  public VolunteerEventAssignment saveAssignment(VolunteerEventAssignment assignment) {
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
        ps.setInt(4, assignment.getId());
        ps.executeUpdate();
        return assignment;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not save volunteer assignment", e);
    }
  }

  @Override
  public VolunteerEventAssignment findAssignmentById(int id) {
    try (
      PreparedStatement ps = connection.prepareStatement(ASSIGNMENT_SELECT + " WHERE vh.id=?")
    ) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? mapAssignment(rs) : null;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load volunteer assignment", e);
    }
  }

  @Override
  public List<VolunteerEventAssignment> findAssignments(int volunteerId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        ASSIGNMENT_SELECT + " WHERE vh.volunteer_id=? ORDER BY vh.work_date DESC, vh.id DESC"
      )
    ) {
      ps.setInt(1, volunteerId);
      try (ResultSet rs = ps.executeQuery()) {
        return readAssignments(rs);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list volunteer assignments", e);
    }
  }

  @Override
  public boolean hasAssignment(int volunteerId, int eventId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "SELECT 1 FROM volunteer_hours WHERE volunteer_id=? AND event_id=?"
      )
    ) {
      ps.setInt(1, volunteerId);
      ps.setInt(2, eventId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not check volunteer assignment", e);
    }
  }

  //
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
        volunteer.setId(keys.getInt(1));
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

  private List<Volunteer> findVolunteers(String sql) {
    try (
      PreparedStatement ps = connection.prepareStatement(sql);
      ResultSet rs = ps.executeQuery()
    ) {
      return readVolunteers(rs);
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list volunteers", e);
    }
  }

  private List<Volunteer> readVolunteers(ResultSet rs) throws SQLException {
    List<Volunteer> volunteers = new ArrayList<>();
    while (rs.next()) volunteers.add(mapVolunteer(rs));
    return volunteers;
  }

  private List<Skill> readSkills(ResultSet rs) throws SQLException {
    List<Skill> skills = new ArrayList<>();
    while (rs.next()) skills.add(mapSkill(rs));
    return skills;
  }

  private List<VolunteerEventAssignment> readAssignments(ResultSet rs) throws SQLException {
    List<VolunteerEventAssignment> assignments = new ArrayList<>();
    while (rs.next()) assignments.add(mapAssignment(rs));
    return assignments;
  }

  private VolunteerEventAssignment insertAssignment(VolunteerEventAssignment assignment)
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
      ps.setInt(1, assignment.getVolunteerId());
      ps.setInt(2, assignment.getEventId());
      ps.setInt(3, assignment.getSkillId());
      ps.setString(4, assignment.getWorkDate().toString());
      ps.setDouble(5, assignment.getHours());
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No assignment id returned");
        assignment.setId(keys.getInt(1));
        return assignment;
      }
    }
  }


  //helpers
  private Volunteer mapVolunteer(ResultSet rs) throws SQLException {
    Volunteer volunteer = new Volunteer(
      rs.getString("name"),
      rs.getString("email"),
      rs.getString("phone"),
      rs.getInt("available") == 1
    );
    volunteer.setId(rs.getInt("id"));
    return volunteer;
  }

  private Skill mapSkill(ResultSet rs) throws SQLException {
    return new Skill(rs.getInt("id"), rs.getString("name"));
  }

  private VolunteerEventAssignment mapAssignment(ResultSet rs) throws SQLException {
    VolunteerEventAssignment assignment = new VolunteerEventAssignment(
      rs.getInt("id"),
      rs.getInt("volunteer_id"),
      rs.getInt("event_id"),
      rs.getInt("skill_id"),
      LocalDate.parse(rs.getString("work_date")),
      rs.getInt("hours")
    );
    assignment.setEventTitle(rs.getString("event_title"));
    assignment.setSkillName(rs.getString("skill_name"));
    return assignment;
  }
}
