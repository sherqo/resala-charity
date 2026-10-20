package com.resala.model;

/**
 * A login account.
 */
// TODO: enforce volunteerId for VOLUNTEER only
public class Account {

  private long id;
  private String username;
  private String passwordHash;
  private String fullName;
  private String email;
  private Role role;
  private Long volunteerId; // null if ADMIN or STAFF, has value if VOLUNTEER

  // constructors
  public Account() {}

  public Account(
    long id,
    String username,
    String passwordHash,
    String fullName,
    String email,
    Role role,
    Long volunteerId
  ) {
    this.id = id;
    this.username = username;
    this.passwordHash = passwordHash;
    this.fullName = fullName;
    this.email = email;
    this.role = role;
    this.volunteerId = volunteerId;
  }

  // getters & Setters
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public Long getVolunteerId() {
    return volunteerId;
  }

  public void setVolunteerId(Long volunteerId) {
    this.volunteerId = volunteerId;
  }
}
