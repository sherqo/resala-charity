package com.resala.volunteer;

import com.resala.event.EventRepository;
import com.resala.model.Event;
import com.resala.model.Skill;
import com.resala.model.Volunteer;
import com.resala.model.VolunteerEventAssignment;
import com.resala.volunteer.helpers.CertificateGenerator;
import com.resala.volunteer.helpers.VolunteerSummary;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VolunteerService {

  private final VolunteerRepository volunteers;
  private final EventRepository events;
  private final CertificateGenerator certificateGenerator;

  public VolunteerService(VolunteerRepository volunteers, EventRepository events) {
    this.volunteers = volunteers;
    this.events = events;
    this.certificateGenerator = new CertificateGenerator();
  }

  // Volunteer CRUD
  public Volunteer create(Volunteer request) {
    // to ensure name is not empty 
    validate(request);
    Volunteer volunteer = new Volunteer(
      request.getName().trim(),
      isNull(request.getEmail()),
      isNull(request.getPhone()),
      request.isAvailable()
    );
    return volunteers.save(volunteer);
  }

  public Volunteer update(int id, Volunteer request) {
    Volunteer volunteer = getVolunteer(id);
    validate(request);

    volunteer.setName(request.getName().trim());
    volunteer.setEmail(isNull(request.getEmail()));
    volunteer.setPhone(isNull(request.getPhone()));
    volunteer.setAvailable(request.isAvailable());
    return volunteers.save(volunteer);
  }

  public void delete(int id) {
    getVolunteer(id);
    volunteers.delete(id);
  }

  public void setAvailability(int id, boolean available) {
    getVolunteer(id);
    volunteers.setAvailability(id, available);
  }

  public List<Volunteer> listVolunteers() {
    return volunteers.findAll();
  }

  public Volunteer getVolunteer(int id) {
    Volunteer volunteer = volunteers.findById(id);
    if (volunteer == null) {
      throw new IllegalArgumentException("Volunteer not found: #" + id);
    }
    return volunteer;
  }

  // Skills
  public List<Skill> listSkills() {
    return volunteers.findAllSkills();
  }

  public Skill getSkill(int skillId) {
    Skill skill = volunteers.findSkillById(skillId);
    if (skill == null) {
      throw new IllegalArgumentException("Skill not found: #" + skillId);
    }
    return skill;
  }

  public Skill createSkill(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Skill name is required");
    }

    String cleanName = name.trim();
    if (volunteers.findSkillByName(cleanName) != null) {
      throw new IllegalStateException("Skill already exists: " + cleanName);
    }

    return volunteers.saveSkill(new Skill(0, cleanName));
  }

  public void deleteSkill(int skillId) {
    getSkill(skillId);
    if (volunteers.isSkillInUse(skillId)) {
      throw new IllegalStateException(
        "Cannot delete skill #" + skillId +
        ": it is assigned to volunteers or used in an assignment"
      );
    }
    volunteers.deleteSkill(skillId);
  }

  // Volunteer Skills
  public List<Skill> getVolunteerSkills(int volunteerId) {
    getVolunteer(volunteerId);
    return volunteers.findSkillsForVolunteer(volunteerId);
  }

  public void addSkill(int volunteerId, int skillId) {
    getVolunteer(volunteerId);
    getSkill(skillId);
    volunteers.addSkill(volunteerId, skillId);
  }

  public void removeSkill(int volunteerId, int skillId) {
    getVolunteer(volunteerId);
    volunteers.removeSkill(volunteerId, skillId);
  }

  public List<Volunteer> searchBySkill(int skillId) {
    getSkill(skillId);
    return volunteers.findBySkill(skillId);
  }

  public List<Volunteer> listAvailableVolunteers() {
    return volunteers.findAvailable();
  }


  // Assignments
  public List<Event> listEvents() {
    return events.findAll();
  }

  public VolunteerEventAssignment assign(int volunteerId, int eventId, int skillId) {
    Volunteer volunteer = getVolunteer(volunteerId);
    Event event = getEvent(eventId);
    Skill skill = getSkill(skillId);

    validateAssignment(volunteer, event, skillId);

    VolunteerEventAssignment assignment = new VolunteerEventAssignment(
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

  public VolunteerEventAssignment getAssignment(int assignmentId) {
    VolunteerEventAssignment assignment = volunteers.findAssignmentById(assignmentId);
    if (assignment == null) {
      throw new IllegalArgumentException("Assignment not found: #" + assignmentId);
    }
    return assignment;
  }

  public List<VolunteerEventAssignment> listAssignments(int volunteerId) {
    getVolunteer(volunteerId);
    return volunteers.findAssignments(volunteerId);
  }

  //add event hours to volunteer
  public VolunteerEventAssignment logHours(int assignmentId, int hours) {
    if (hours <= 0) throw new IllegalArgumentException("Hours must be greater than zero");

    VolunteerEventAssignment assignment = getAssignment(assignmentId);
    assignment.setHours(hours);
    return volunteers.saveAssignment(assignment);
  }

  // Summaries and certificate
  public VolunteerSummary summary(int volunteerId) {
    getVolunteer(volunteerId);

    List<VolunteerEventAssignment> assignments = volunteers.findAssignments(volunteerId);
    double total = 0;
    Map<String, Double> byEvent = new LinkedHashMap<>();

    for (VolunteerEventAssignment assignment : assignments) {
      if (assignment.getHours() <= 0) continue;

      total += assignment.getHours();
      String eventName = assignment.getEventTitle() == null
        ? "Event #" + assignment.getEventId()
        : assignment.getEventTitle();
      byEvent.merge(eventName, assignment.getHours(), Double::sum);
    }

    return new VolunteerSummary(total, byEvent);
  }

  public String certificate(int volunteerId) {
    Volunteer volunteer = getVolunteer(volunteerId);
    return certificateGenerator.generate(volunteer, summary(volunteerId));
  }

  // Private helpers
  private Event getEvent(int eventId) {
    for (Event event : events.findAll()) {
      if (event.getId() == eventId) return event;
    }
    throw new IllegalArgumentException("Event not found: #" + eventId);
  }

  private void validateAssignment(Volunteer volunteer, Event event, int skillId) {
    if (!volunteer.isAvailable()) {
      throw new IllegalStateException("Volunteer is not available");
    }
    if (event.getStatus() == Event.Status.CLOSED) {
      throw new IllegalStateException("Cannot assign a volunteer to a closed event");
    }
    if (event.getDate().isBefore(LocalDate.now())) {
      throw new IllegalStateException("Cannot assign a volunteer to a past event");
    }
    if (volunteers.hasAssignment(volunteer.getId(), (int) event.getId())) {
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

  private String isNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
