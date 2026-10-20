package com.resala.model;

import java.time.LocalDate;

/** An organized event by the charity */
public class Event {

  // regestration status
  public enum Status {
    DRAFT,
    OPEN,
    CLOSED,
  }

  private long id;
  private String title;
  private int eventTypeId; // DB: event_types.id
  private LocalDate date;
  private int capacity;
  private Status status;
  private String description;

  // constructors
  public Event() {}

  public Event(
    long id,
    String title,
    int eventTypeId,
    LocalDate date,
    int capacity,
    Status status,
    String description
  ) {
    this.id = id;
    this.title = title;
    this.eventTypeId = eventTypeId;
    this.date = date;
    this.capacity = capacity;
    this.status = status;
    this.description = description;
  }

  // getters & setters
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public int getEventTypeId() {
    return eventTypeId;
  }

  public void setEventTypeId(int eventTypeId) {
    this.eventTypeId = eventTypeId;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
