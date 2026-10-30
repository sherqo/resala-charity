package com.resala.volunteer;

import java.time.LocalDate;

/**
 * One volunteer assignment to an event.
 * The same row stores the hours logged after the volunteer finishes.
 * It is persisted in the volunteer_hours table.
 */
public class VolunteerAssignment {

  private long id;
  private long volunteerId;
  private long eventId;
  private int skillId;
  private LocalDate workDate;
  private double hours;

  // Display-only values filled by the repository from a JOIN.
  private String eventTitle;
  private String skillName;

  public VolunteerAssignment() {}

  public VolunteerAssignment(
    long id,
    long volunteerId,
    long eventId,
    int skillId,
    LocalDate workDate,
    double hours
  ) {
    this.id = id;
    this.volunteerId = volunteerId;
    this.eventId = eventId;
    this.skillId = skillId;
    this.workDate = workDate;
    this.hours = hours;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getVolunteerId() {
    return volunteerId;
  }

  public void setVolunteerId(long volunteerId) {
    this.volunteerId = volunteerId;
  }

  public long getEventId() {
    return eventId;
  }

  public void setEventId(long eventId) {
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

  public void setHours(double hours) {
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
