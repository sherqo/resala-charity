package com.resala.beneficiary;

import java.time.LocalDate;

/** Record of aid or items distributed to a beneficiary. */
public class Distribution {

  private long id;
  private long beneficiaryId;
  private Long donationId; // nullable: optional link to an in-kind donation
  private String itemDescription;
  private int quantity;
  private LocalDate distributionDate;
  private String notes;

  public Distribution() {}

  public Distribution(
      long id,
      long beneficiaryId,
      Long donationId,
      String itemDescription,
      int quantity,
      LocalDate distributionDate,
      String notes
  ) {
    this.id = id;
    this.beneficiaryId = beneficiaryId;
    this.donationId = donationId;
    this.itemDescription = itemDescription;
    this.quantity = quantity;
    this.distributionDate = distributionDate;
    this.notes = notes;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getBeneficiaryId() {
    return beneficiaryId;
  }

  public void setBeneficiaryId(long beneficiaryId) {
    this.beneficiaryId = beneficiaryId;
  }

  public Long getDonationId() {
    return donationId;
  }

  public void setDonationId(Long donationId) {
    this.donationId = donationId;
  }

  public String getItemDescription() {
    return itemDescription;
  }

  public void setItemDescription(String itemDescription) {
    this.itemDescription = itemDescription;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public LocalDate getDistributionDate() {
    return distributionDate;
  }

  public void setDistributionDate(LocalDate distributionDate) {
    this.distributionDate = distributionDate;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
