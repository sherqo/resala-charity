package com.resala.volunteer;

import com.resala.event.EventRepository;
import com.resala.model.Event;
import com.resala.model.Skill;
import com.resala.persistence.sqlite.SqliteVolunteerRepository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Business logic for volunteers, skills, assignments, hours, and certificates. */
public class VolunteerService {

  private final SqliteVolunteerRepository volunteers;
  private final EventRepository events;
  private final CertificateGenerator certificateGenerator;

  public VolunteerService(SqliteVolunteerRepository volunteers, EventRepository events) {
    this.volunteers = volunteers;
    this.events = events;
    this.certificateGenerator = new CertificateGenerator();
  }

  // ── Volunteer CRUD ─────────────────────────────────────────────────────────

  public com.resala.model.Volunteer create(Volunteer request) {
    validate(request);
    com.resala.model.Volunteer volunteer = new com.resala.model.Volunteer(
      0,
      request.getName().trim(),
      blankToNull(request.getEmail()),
      blankToNull(request.getPhone()),
      request.isAvailable()
    );
    return volunteers.save(volunteer);
  }

  public com.resala.model.Volunteer update(long id, Volunteer request) {
    com.resala.model.Volunteer volunteer = getVolunteer(id);
    validate(request);

    volunteer.setName(request.getName().trim());
    volunteer.setEmail(blankToNull(request.getEmail()));
    volunteer.setPhone(blankToNull(request.getPhone()));
    volunteer.setAvailable(request.isAvailable());
    return volunteers.save(volunteer);
  }

  public void delete(long id) {
    getVolunteer(id);
    volunteers.delete(id);
  }

  public void setAvailability(long id, boolean available) {
    getVolunteer(id);
    volunteers.setAvailability(id, available);
  }

  public List<com.resala.model.Volunteer> listVolunteers() {
    return volunteers.findAll();
  }

  public com.resala.model.Volunteer getVolunteer(long id) {
    return volunteers
      .findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Volunteer not found: #" + id));
  }

  // ── Skills ─────────────────────────────────────────────────────────────────

  public List<Skill> listSkills() {
    return volunteers.findAllSkills();
  }

  public Skill getSkill(int skillId) {
    return volunteers
      .findSkillById(skillId)
      .orElseThrow(() -> new IllegalArgumentException("Skill not found: #" + skillId));
  }

  public List<Skill> getVolunteerSkills(long volunteerId) {
    getVolunteer(volunteerId);
    return volunteers.findSkillsForVolunteer(volunteerId);
  }

  public void addSkill(long volunteerId, int skillId) {
    getVolunteer(volunteerId);
    getSkill(skillId);
    volunteers.addSkill(volunteerId, skillId);
  }

  public void removeSkill(long volunteerId, int skillId) {
    getVolunteer(volunteerId);
    volunteers.removeSkill(volunteerId, skillId);
  }

  public List<com.resala.model.Volunteer> searchBySkill(int skillId) {
    getSkill(skillId);
    return volunteers.findBySkill(skillId);
  }

  public List<com.resala.model.Volunteer> listAvailableVolunteers() {
    return volunteers.findAvailable();
  }

  // ── Assignments ────────────────────────────────────────────────────────────

  public List<Event> listEvents() {
    return events.findAll();
  }

  public VolunteerAssignment assign(long volunteerId, long eventId, int skillId) {
    com.resala.model.Volunteer volunteer = getVolunteer(volunteerId);
    Event event = getEvent(eventId);
    Skill skill = getSkill(skillId);

    validateAssignment(volunteer, event, skillId);

    VolunteerAssignment assignment = new VolunteerAssignment(
      0,
      volunteerId,
      eventId,
      skillId,
      event.getDate(),
      0
    );
    assignment.setEventTitle(event.getTitle());
    assignment.setSkillName(skill.getName());
    return volunteers.saveAssignment(assignment);
  }

  public VolunteerAssignment getAssignment(long assignmentId) {
    return volunteers
      .findAssignmentById(assignmentId)
      .orElseThrow(() -> new IllegalArgumentException("Assignment not found: #" + assignmentId));
  }

  public List<VolunteerAssignment> listAssignments(long volunteerId) {
    getVolunteer(volunteerId);
    return volunteers.findAssignments(volunteerId);
  }

  /** Logs (or corrects) the hours for one assignment. */
  public VolunteerAssignment logHours(long assignmentId, double hours) {
    if (hours <= 0) throw new IllegalArgumentException("Hours must be greater than zero");

    VolunteerAssignment assignment = getAssignment(assignmentId);
    assignment.setHours(hours);
    return volunteers.saveAssignment(assignment);
  }

  // ── Summaries and certificate ──────────────────────────────────────────────

  public VolunteerSummary summary(long volunteerId) {
    getVolunteer(volunteerId);

    List<VolunteerAssignment> assignments = volunteers.findAssignments(volunteerId);
    double total = 0;
    Map<String, Double> byEvent = new LinkedHashMap<>();

    for (VolunteerAssignment assignment : assignments) {
      if (assignment.getHours() <= 0) continue;

      total += assignment.getHours();
      String eventName = assignment.getEventTitle() == null
        ? "Event #" + assignment.getEventId()
        : assignment.getEventTitle();
      byEvent.merge(eventName, assignment.getHours(), Double::sum);
    }

    return new VolunteerSummary(total, byEvent);
  }

  public String certificate(long volunteerId) {
    com.resala.model.Volunteer volunteer = getVolunteer(volunteerId);
    return certificateGenerator.generate(volunteer, summary(volunteerId));
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private Event getEvent(long eventId) {
    return events
      .findById(eventId)
      .orElseThrow(() -> new IllegalArgumentException("Event not found: #" + eventId));
  }

  private void validateAssignment(com.resala.model.Volunteer volunteer, Event event, int skillId) {
    if (!volunteer.isAvailable()) {
      throw new IllegalStateException("Volunteer is not available");
    }
    if (event.getStatus() == Event.Status.CLOSED) {
      throw new IllegalStateException("Cannot assign a volunteer to a closed event");
    }
    if (event.getDate().isBefore(LocalDate.now())) {
      throw new IllegalStateException("Cannot assign a volunteer to a past event");
    }
    if (volunteers.hasAssignment(volunteer.getId(), event.getId())) {
      throw new IllegalStateException("Volunteer is already assigned to this event");
    }

    boolean hasSkill = volunteers
      .findSkillsForVolunteer(volunteer.getId())
      .stream()
      .map(Skill::getId)
      .anyMatch(id -> id == skillId);

    if (!hasSkill) {
      throw new IllegalStateException("Volunteer does not have the required skill");
    }
  }

  private void validate(Volunteer request) {
    if (request.getName() == null || request.getName().isBlank()) {
      throw new IllegalArgumentException("Volunteer name is required");
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
