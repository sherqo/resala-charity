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
}
