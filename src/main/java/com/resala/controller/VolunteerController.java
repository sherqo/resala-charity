package com.resala.controller;

import com.resala.model.Event;
import com.resala.model.Skill;
import com.resala.model.Volunteer;
import com.resala.model.VolunteerEventAssignment;
import com.resala.volunteer.VolunteerService;
import com.resala.volunteer.helpers.VolunteerSummary;
import java.util.List;

public class VolunteerController {

  private final VolunteerService service;

  public VolunteerController(VolunteerService service) {
    this.service = service;
  }

  public Volunteer create(Volunteer request) {
    return service.create(request);
  }

  public Volunteer update(int id, Volunteer request) {
    return service.update(id, request);
  }

  public void delete(int id) {
    service.delete(id);
  }

  public void setAvailability(int id, boolean available) {
    service.setAvailability(id, available);
  }

  public List<Volunteer> listVolunteers() {
    return service.listVolunteers();
  }

  public Volunteer getVolunteer(int id) {
    return service.getVolunteer(id);
  }

  public List<Skill> listSkills() {
    return service.listSkills();
  }

  public Skill getSkill(int skillId) {
    return service.getSkill(skillId);
  }

  public Skill createSkill(String name) {
    return service.createSkill(name);
  }

  public void deleteSkill(int skillId) {
    service.deleteSkill(skillId);
  }

  public List<Skill> getVolunteerSkills(int volunteerId) {
    return service.getVolunteerSkills(volunteerId);
  }

  public void addSkill(int volunteerId, int skillId) {
    service.addSkill(volunteerId, skillId);
  }

  public void removeSkill(int volunteerId, int skillId) {
    service.removeSkill(volunteerId, skillId);
  }

  public List<Volunteer> searchBySkill(int skillId) {
    return service.searchBySkill(skillId);
  }

  public List<Volunteer> listAvailableVolunteers() {
    return service.listAvailableVolunteers();
  }

  public List<Event> listEvents() {
    return service.listEvents();
  }

  public VolunteerEventAssignment assign(int volunteerId, int eventId, int skillId) {
    return service.assign(volunteerId, eventId, skillId);
  }

  public List<VolunteerEventAssignment> listAssignments(int volunteerId) {
    return service.listAssignments(volunteerId);
  }

  public VolunteerEventAssignment logHours(int assignmentId, int hours) {
    return service.logHours(assignmentId, hours);
  }

  public VolunteerSummary summary(int volunteerId) {
    return service.summary(volunteerId);
  }

  public String certificate(int volunteerId) {
    return service.certificate(volunteerId);
  }
}
