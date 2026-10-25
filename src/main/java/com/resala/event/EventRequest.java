package com.resala.event;

import java.time.LocalDate;

/**
 * DTO for creating or updating an event.
 * Views build this and pass it to EventController — no model objects cross the boundary.
 */
public record EventRequest(
  String title,
  int eventTypeId,
  LocalDate date,
  int capacity,
  String description
) {}
