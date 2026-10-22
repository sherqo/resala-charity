package com.resala.donation;

import com.resala.model.DonationType;
import java.time.LocalDate;

public final class OnlineDonation extends Donation {

  private final long amountMinor;

  public OnlineDonation(long donorId, long amountMinor, LocalDate date, String reference, String notes) {
    super(donorId, DonationType.ONLINE, date, reference, notes);
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
