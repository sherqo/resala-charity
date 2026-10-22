package com.resala.model;

/**
 * A volunteer profile. Skills and logged hours live in separate tables
 * an account is optional and can be linked to a volunteer from the account itself
 * i.e. an account can point on a volunteer not the other way aroundaa
 *
 * <p>{@code hours} is a derived total: the repository loads it as a SUM over the
 * volunteer_hours log rows. It is never accumulated in memory, so there is no
 * addHours() method to call.
 */
public class Volunteer {

  private long id;
  private String name;
  private String email;
  private String phone;
  private boolean available;
  private double hours;

  // constructors
  public Volunteer() {}

  public Volunteer(
    long id,
    String name,
    String email,
    String phone,
    boolean available,
    double hours
  ) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.phone = phone;
    this.available = available;
    this.hours = hours;
  }

  // getters & setters
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  /** Total hours logged, derived from the volunteer_hours log rows. */
  public double getHours() {
    return hours;
  }

  public void setHours(double hours) {
    this.hours = hours;
  }
}
