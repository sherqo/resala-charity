package com.resala.donation;

import com.resala.model.DonationType;
import java.time.LocalDate;

// this doensn't care about the used UI
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
