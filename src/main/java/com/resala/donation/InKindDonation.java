package com.resala.donation;

import com.resala.model.DonationType;
import java.time.LocalDate;

public final class InKindDonation extends Donation {

  private final String itemDescription;
  private final int quantity;
  private final Long estimatedValueMinor;

  public InKindDonation(
    long donorId,
    String itemDescription,
    int quantity,
    Long estimatedValueMinor,
    LocalDate date,
    String notes
  ) {
    super(donorId, DonationType.IN_KIND, date, null, notes);
    this.itemDescription = itemDescription;
    this.quantity = quantity;
    this.estimatedValueMinor = estimatedValueMinor;
  }

  public long getAmountMinor() {
    return 0;
  }

  public String getItemDescription() {
    return itemDescription;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public Long getEstimatedValueMinor() {
    return estimatedValueMinor;
  }
}
