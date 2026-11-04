package com.resala.donation;

import com.resala.model.DonationType;
import java.time.LocalDate;

public record DonationRequest(
  long donorId,
  DonationType type,
  Long amountMinor,
  String itemDescription,
  Integer quantity,
  Long estimatedValueMinor,
  String referenceNumber,
  LocalDate donationDate,
  String notes
) {}
