package com.resala.volunteer;

import com.resala.core.AuditLogManager;
import com.resala.event.EventRegistration;
import com.resala.event.EventRegistrationRepository;
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
import java.util.Optional;
import java.util.UUID;

public class VolunteerService {

  private final VolunteerRepository volunteers;
  private final EventRepository events;
  private final EventRegistrationRepository eventRegistrations;
  private final CertificateGenerator certificateGenerator;

  public VolunteerService(
    VolunteerRepository volunteers,
    EventRepository events,
    EventRegistrationRepository eventRegistrations
  ) {
    this.volunteers = volunteers;
    this.events = events;
    this.eventRegistrations = eventRegistrations;
    this.certificateGenerator = new CertificateGenerator();
  }

  public VolunteerService(VolunteerRepository volunteers, EventRepository events) {
    this(volunteers, events, null);
  }

  public Volunteer create(Volunteer request) {

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

  public List<Event> listEvents() {
    return events.findAll();
  }

  public List<Event> listOpenEvents() {
    return events.findAll().stream()
      .filter(e -> e.getStatus() == Event.Status.OPEN)
      .filter(e -> !e.getDate().isBefore(LocalDate.now()))
      .filter(e -> eventRegistrations == null || eventRegistrations.countRegistered(e.getId()) < e.getCapacity())
      .toList();
  }

  public VolunteerEventAssignment assign(int volunteerId, int eventId, int skillId) {
    Volunteer volunteer = getVolunteer(volunteerId);
    Event event = getEvent(eventId);
    Skill skill = getSkill(skillId);

    validateAssignment(volunteer, event, skill);

    // Count all registrations before checking capacity.
    if (eventRegistrations != null) {
      int registered = eventRegistrations.countRegistered(eventId);
      if (registered >= event.getCapacity()) {
        throw new IllegalStateException("Event is full (" + event.getCapacity() + " spots)");
      }
    }

    // Volunteers are also stored as event attendees.
    if (eventRegistrations != null) {
      Optional<EventRegistration> existing = eventRegistrations.findByVolunteerAndEvent(volunteerId, eventId);
      if (existing.isEmpty()) {
        String ticketRef = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        EventRegistration reg = new EventRegistration(
          0,
          eventId,
          volunteer.getName(),
          volunteer.getEmail(),
          ticketRef,
          false,
          volunteerId,
          skillId
        );
        eventRegistrations.register(reg);
      }
    }

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
    VolunteerEventAssignment saved = volunteers.saveAssignment(assignment);

    AuditLogManager.getInstance().log(
        "Unified Registration",
        "Volunteers / Events",
        "Observer Triggers",
        volunteer.getName() + " (" + volunteer.getEmail() + ")",
        "Volunteer registered to " + event.getTitle(),
        "Volunteer registered for Event #" + event.getId() + " (" + event.getTitle() + ")\n"
        + "Assigned Skill: " + skill.getName() + " (ID: #" + skill.getId() + ")\n"
        + "Capacity Check: passed\n"
        + "Volunteer Availability: confirmed (true)\n"
        + "Skill Requirement: confirmed"
    );

    return saved;
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

  public Optional<EventRegistration> getEventRegistration(int volunteerId, long eventId) {
    if (eventRegistrations == null) return Optional.empty();
    return eventRegistrations.findByVolunteerAndEvent(volunteerId, eventId);
  }

  public int creditVolunteerHours(Event event, int volunteerId) {
    if (event == null || event.getHours() <= 0) {
      return 0;
    }

    int credited = volunteers.creditVolunteerHours(
      (int) event.getId(), volunteerId, event.getHours());
    if (credited > 0) {
      AuditLogManager.getInstance().log(
          "Event Check-In",
          "Volunteers / Events",
          "Observer Triggers",
          "Volunteer #" + volunteerId,
          "Hours credited for " + event.getTitle(),
          "Event #" + event.getId() + " (" + event.getTitle() + ") check-in.\n"
          + "Credited " + event.getHours() + " hour(s) to volunteer #" + volunteerId + "."
      );
    }
    return credited;
  }

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

  private Event getEvent(int eventId) {
    return events.findById(eventId)
      .orElseThrow(() -> new IllegalArgumentException("Event not found: #" + eventId));
  }

  private void validateAssignment(Volunteer volunteer, Event event, Skill skill) {
    if (!volunteer.isAvailable()) {
      throw new IllegalStateException("Volunteer is not available for assignment");
    }
    boolean hasSkill = volunteers
      .findSkillsForVolunteer(volunteer.getId())
      .stream()
      .map(Skill::getId)
      .anyMatch(id -> id == skill.getId());
    if (!hasSkill) {
      throw new IllegalStateException(
        "Volunteer does not have the required skill: " + skill.getName()
      );
    }
    if (event.getStatus() == Event.Status.DRAFT) {
      throw new IllegalStateException("Cannot register a volunteer for a draft event");
    }
    if (event.getStatus() == Event.Status.CLOSED) {
      throw new IllegalStateException("Cannot register a volunteer for a closed event");
    }
    if (event.getDate().isBefore(LocalDate.now())) {
      throw new IllegalStateException("Cannot register a volunteer for a past event");
    }
    if (volunteers.hasAssignment(volunteer.getId(), (int) event.getId())) {
      throw new IllegalStateException("Volunteer is already registered for this event");
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
