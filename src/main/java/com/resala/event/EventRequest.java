package com.resala.event;

import java.time.LocalDate;

public record EventRequest(
  String title,
  int eventTypeId,
  LocalDate date,
  int capacity,
  int hours,
  String description
) {

  public EventRequest(
    String title,
    int eventTypeId,
    LocalDate date,
    int capacity,
    String description
  ) {
    this(title, eventTypeId, date, capacity, 0, description);
  }
}
