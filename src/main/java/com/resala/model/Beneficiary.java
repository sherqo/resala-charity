package com.resala.model;

public class Beneficiary {

  public enum Status {
    ACTIVE,
    CLOSED,
  }

  private long id;
  private String name;
  private String phone;
  private int householdSize;
  private int needTypeId;
  private Status status;
  private String notes;
  private int impactScore;

  public Beneficiary() {}

  public Beneficiary(
    long id,
    String name,
    String phone,
    int householdSize,
    int needTypeId,
    Status status,
    String notes
  ) {
    this(id, name, phone, householdSize, needTypeId, status, notes, 0);
  }

  public Beneficiary(
    long id,
    String name,
    String phone,
    int householdSize,
    int needTypeId,
    Status status,
    String notes,
    int impactScore
  ) {
    this.id = id;
    this.name = name;
    this.phone = phone;
    this.householdSize = householdSize;
    this.needTypeId = needTypeId;
    this.status = status;
    this.notes = notes;
    this.impactScore = impactScore;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public int getHouseholdSize() {
    return householdSize;
  }

  public void setHouseholdSize(int householdSize) {
    this.householdSize = householdSize;
  }

  public int getNeedTypeId() {
    return needTypeId;
  }

  public void setNeedTypeId(int needTypeId) {
    this.needTypeId = needTypeId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public int getImpactScore() {
    return impactScore;
  }

  public void setImpactScore(int impactScore) {
    this.impactScore = impactScore;
  }
}
