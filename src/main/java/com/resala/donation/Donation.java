package com.resala.donation;

import com.resala.model.DonationType;
import java.time.LocalDate;

// Factory
public abstract class Donation {

  private long id;
  private final long donorId;
  private final DonationType type;
  private final LocalDate donationDate;
  private final String notes;
  private String referenceNumber;

  protected Donation(long donorId, DonationType type, LocalDate donationDate, String referenceNumber, String notes) {
    this.donorId = donorId;
    this.type = type;
    this.donationDate = donationDate;
    this.referenceNumber = referenceNumber;
    this.notes = notes;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getDonorId() {
    return donorId;
  }

  public DonationType getType() {
    return type;
  }

  public LocalDate getDonationDate() {
    return donationDate;
  }

  public String getReferenceNumber() {
    return referenceNumber;
  }

  public void setReferenceNumber(String referenceNumber) {
    this.referenceNumber = referenceNumber;
  }

  public String getNotes() {
    return notes;
  }

  public abstract long getAmountMinor();

  public abstract String getItemDescription();

  public abstract Integer getQuantity();

  public abstract Long getEstimatedValueMinor();
}
