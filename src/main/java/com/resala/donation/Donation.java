package com.resala.donation;

import com.resala.model.DonationType;
import java.time.LocalDate;

// abstract product — DonationFactory is the Creator
public abstract class Donation {

  private long id;
  private final long donorId;
  private final DonationType type;
  private final LocalDate donationDate;
  private final String notes;
  private String referenceNumber;
  private DonationStatus status;

  protected Donation(long donorId, DonationType type, LocalDate donationDate, String referenceNumber, String notes) {
    this(donorId, type, donationDate, referenceNumber, notes, DonationStatus.COMPLETED);
  }

  protected Donation(long donorId, DonationType type, LocalDate donationDate, String referenceNumber, String notes,
                     DonationStatus status) {
    this.donorId = donorId;
    this.type = type;
    this.donationDate = donationDate;
    this.referenceNumber = referenceNumber;
    this.notes = notes;
    this.status = status;
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

  public DonationStatus getStatus() {
    return status;
  }

  public void setStatus(DonationStatus status) {
    this.status = status;
  }

  public String getNotes() {
    return notes;
  }

  public abstract long getAmountMinor();

  public abstract String getItemDescription();

  public abstract Integer getQuantity();

  public abstract Long getEstimatedValueMinor();
}
