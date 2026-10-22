package com.resala.donation;

import java.util.Objects;

// Factory Method creator
public class DonationFactory {

  public Donation create(DonationRequest request) {
    Objects.requireNonNull(request, "Donation details are required");
    if (request.donorId() <= 0) throw new IllegalArgumentException("Choose a donor");
    if (request.donationDate() == null) throw new IllegalArgumentException("Donation date is required");
    if (request.type() == null) throw new IllegalArgumentException("Choose a donation type");

    return switch (request.type()) {
      case ONLINE -> createOnline(request);
      case CHECK -> createCheck(request);
      case IN_KIND -> createInKind(request);
    };
  }

  private Donation createOnline(DonationRequest request) {
    if (request.amountMinor() == null || request.amountMinor() <= 0) {
      throw new IllegalArgumentException("Online donation amount must be positive");
    }
    return new OnlineDonation(
      request.donorId(),
      request.amountMinor(),
      request.donationDate(),
      request.referenceNumber(),
      request.notes()
    );
  }

  private Donation createCheck(DonationRequest request) {
    if (request.amountMinor() == null || request.amountMinor() <= 0) {
      throw new IllegalArgumentException("Check donation amount must be positive");
    }
    if (request.referenceNumber() == null || request.referenceNumber().isBlank()) {
      throw new IllegalArgumentException("Check number is required");
    }
    return new CheckDonation(
      request.donorId(),
      request.amountMinor(),
      request.donationDate(),
      request.referenceNumber(),
      request.notes()
    );
  }

  private Donation createInKind(DonationRequest request) {
    if (request.itemDescription() == null || request.itemDescription().isBlank()) {
      throw new IllegalArgumentException("Item description is required");
    }
    if (request.quantity() == null || request.quantity() <= 0) {
      throw new IllegalArgumentException("Item quantity must be positive");
    }
    if (request.estimatedValueMinor() != null && request.estimatedValueMinor() < 0) {
      throw new IllegalArgumentException("Estimated value cannot be negative");
    }
    return new InKindDonation(
      request.donorId(),
      request.itemDescription().trim(),
      request.quantity(),
      request.estimatedValueMinor(),
      request.donationDate(),
      request.notes()
    );
  }
}
