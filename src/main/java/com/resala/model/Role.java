package com.resala.model;

// TODO: should be extended when RBAC get added
public enum Role {
  ADMIN, // can do everything, and the app has only a single admin and it should be created when the app open for the first time
  STAFF, // can do everything except adding new STAFF
  VOLUNTEER, // a volunteer
}
