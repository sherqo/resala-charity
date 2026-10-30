package com.resala.model;

public class Volunteer {

  private int id;
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

  public int getId() {
    return id;
  }

  public void setId(int id) {
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
}