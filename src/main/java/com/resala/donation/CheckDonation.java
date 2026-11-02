package com.resala.donation;

import com.resala.model.DonationType;
import java.time.LocalDate;

public final class CheckDonation extends Donation {

  private final long amountMinor;

  public CheckDonation(long donorId, long amountMinor, LocalDate date, String reference, String notes) {
    this(donorId, amountMinor, date, reference, notes, DonationStatus.COMPLETED);
  }

  public CheckDonation(long donorId, long amountMinor, LocalDate date, String reference, String notes,
                       DonationStatus status) {
    super(donorId, DonationType.CHECK, date, reference, notes, status);
    this.amountMinor = amountMinor;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public String getItemDescription() {
    return null;
  }

  public Integer getQuantity() {
    return null;
  }

  public Long getEstimatedValueMinor() {
    return null;
  }
}
