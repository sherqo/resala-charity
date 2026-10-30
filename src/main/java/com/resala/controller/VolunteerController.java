package com.resala.controller;

import com.resala.model.Event;
import com.resala.model.Skill;
import com.resala.volunteer.VolunteerAssignment;
import com.resala.volunteer.Volunteer;
import com.resala.volunteer.VolunteerService;
import com.resala.volunteer.VolunteerSummary;
import java.util.List;

public class VolunteerController {

  private final VolunteerService service;

  public VolunteerController(VolunteerService service) {
    this.service = service;
  }

  public com.resala.model.Volunteer create(Volunteer request) {
    return service.create(request);
  }

  public com.resala.model.Volunteer update(long id, Volunteer request) {
    return service.update(id, request);
  }

  public void delete(long id) {
    service.delete(id);
  }

  public void setAvailability(long id, boolean available) {
    service.setAvailability(id, available);
  }

  public List<com.resala.model.Volunteer> listVolunteers() {
    return service.listVolunteers();
  }

  public com.resala.model.Volunteer getVolunteer(long id) {
    return service.getVolunteer(id);
  }

  public List<Skill> listSkills() {
    return service.listSkills();
  }

  public List<Skill> getVolunteerSkills(long volunteerId) {
    return service.getVolunteerSkills(volunteerId);
  }

  public void addSkill(long volunteerId, int skillId) {
    service.addSkill(volunteerId, skillId);
  }

  public void removeSkill(long volunteerId, int skillId) {
    service.removeSkill(volunteerId, skillId);
  }

  public List<com.resala.model.Volunteer> searchBySkill(int skillId) {
    return service.searchBySkill(skillId);
  }

  public List<com.resala.model.Volunteer> listAvailableVolunteers() {
    return service.listAvailableVolunteers();
  }

  public List<Event> listEvents() {
    return service.listEvents();
  }

  public VolunteerAssignment assign(long volunteerId, long eventId, int skillId) {
    return service.assign(volunteerId, eventId, skillId);
  }

  public VolunteerAssignment getAssignment(long assignmentId) {
    return service.getAssignment(assignmentId);
  }

  public List<VolunteerAssignment> listAssignments(long volunteerId) {
    return service.listAssignments(volunteerId);
  }

  public VolunteerAssignment logHours(long assignmentId, double hours) {
    return service.logHours(assignmentId, hours);
  }

  public VolunteerSummary summary(long volunteerId) {
    return service.summary(volunteerId);
  }

  public String certificate(long volunteerId) {
    return service.certificate(volunteerId);
  }
}
