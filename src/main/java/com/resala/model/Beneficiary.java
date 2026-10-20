package com.resala.model;

/** A person or family receiving support. Beneficiaries do not log in. */
// TODO: Reject negative household sizes.
public class Beneficiary {

  public enum Status {
    ACTIVE, // currently taking support
    CLOSED, // support stopped for them
  }

  private long id;
  private String name;
  private String phone;
  private int householdSize; // no of people getting help
  private int needTypeId; // the id of the need they need
  private Status status;
  private String notes;

  // constructors
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
    this.id = id;
    this.name = name;
    this.phone = phone;
    this.householdSize = householdSize;
    this.needTypeId = needTypeId;
    this.status = status;
    this.notes = notes;
  }

  // getters and setters
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
}
