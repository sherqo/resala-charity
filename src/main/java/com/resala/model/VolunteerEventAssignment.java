package com.resala.model;

import java.time.LocalDate;

public class VolunteerEventAssignment {

  private int id;
  private int volunteerId;
  private int eventId;
  private int skillId;
  private LocalDate workDate;
  private int hours;

  private String eventTitle;
  private String skillName;

  public VolunteerEventAssignment() {}

  public VolunteerEventAssignment(
    int id,
    int volunteerId,
    int eventId,
    int skillId,
    LocalDate workDate,
    int hours
  ) {
    this.id = id;
    this.volunteerId = volunteerId;
    this.eventId = eventId;
    this.skillId = skillId;
    this.workDate = workDate;
    this.hours = hours;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getVolunteerId() {
    return volunteerId;
  }

  public void setVolunteerId(int volunteerId) {
    this.volunteerId = volunteerId;
  }

  public int getEventId() {
    return eventId;
  }

  public void setEventId(int eventId) {
    this.eventId = eventId;
  }

  public int getSkillId() {
    return skillId;
  }

  public void setSkillId(int skillId) {
    this.skillId = skillId;
  }

  public LocalDate getWorkDate() {
    return workDate;
  }

  public void setWorkDate(LocalDate workDate) {
    this.workDate = workDate;
  }

  public double getHours() {
    return hours;
  }

  public void setHours(int hours) {
    this.hours = hours;
  }

  public String getEventTitle() {
    return eventTitle;
  }

  public void setEventTitle(String eventTitle) {
    this.eventTitle = eventTitle;
  }

  public String getSkillName() {
    return skillName;
  }

  public void setSkillName(String skillName) {
    this.skillName = skillName;
  }
}
