package com.resala.model;

/** actions that can be shown in the UI and enforced by services. */
public enum Permission {
  DONATION_MANAGE, // ADMIN & STAFF
  VOLUNTEER_MANAGE, // ADMIN & STAFF
  EVENT_MANAGE, // ADMIN & STAFF
  BENEFICIARY_MANAGE, // ADMIN & STAFF
  DISTRIBUTION_MANAGE, // ADMIN & STAFF
  COMMUNICATION_SEND, // ADMIN & STAFF
  REPORT_VIEW, // ADMIN & STAFF
  LOOKUP_MANAGE, // ADMIN only
  ACCOUNT_MANAGE, // ADMIN only
  SELF_SERVICE_VIEW, // VOLUNTEER only
}
