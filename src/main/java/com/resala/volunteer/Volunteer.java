package com.resala.volunteer;

/**
 * Data entered by the view when creating or editing a volunteer.
 * This is a normal class (not a record) so it can be extended later
 * without changing the controller or service signatures.
 */
public class Volunteer {

  private String name;
  private String email;
  private String phone;
  private boolean available;

  public Volunteer() {}

  public Volunteer(String name, String email, String phone, boolean available) {
    this.name = name;
    this.email = email;
    this.phone = phone;
    this.available = available;
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
}
