package com.resala.volunteer;

import java.util.Map;

/** Calculated hour totals for one volunteer. */
public class VolunteerSummary {

  private final double totalHours;
  private final Map<String, Double> hoursByEvent;

  public VolunteerSummary(
    double totalHours,
    Map<String, Double> hoursByEvent
  ) {
    this.totalHours = totalHours;
    this.hoursByEvent = hoursByEvent;
  }

  public double getTotalHours() {
    return totalHours;
  }

  /** Event title -> total hours. */
  public Map<String, Double> getHoursByEvent() {
    return hoursByEvent;
  }

}
