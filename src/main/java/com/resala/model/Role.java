package com.resala.model;

import java.util.EnumSet;

public enum Role {
  ADMIN(EnumSet.allOf(Permission.class)),
  STAFF(
    EnumSet.of(
      Permission.DONATION_MANAGE,
      Permission.VOLUNTEER_MANAGE,
      Permission.EVENT_MANAGE,
      Permission.BENEFICIARY_MANAGE,
      Permission.DISTRIBUTION_MANAGE,
      Permission.COMMUNICATION_SEND,
      Permission.REPORT_VIEW
    )
  ),
  VOLUNTEER(EnumSet.of(Permission.SELF_SERVICE_VIEW));

  private final EnumSet<Permission> permissions;

  Role(EnumSet<Permission> permissions) {
    this.permissions = permissions;
  }

  public boolean can(Permission permission) {
    return permissions.contains(permission);
  }
}
