package com.resala.donation;

/** Lifecycle state of a donation. */
public enum DonationStatus {
  PENDING,
  COMPLETED,
  CANCELLED,
  REFUNDED,
  FAILED;
}
