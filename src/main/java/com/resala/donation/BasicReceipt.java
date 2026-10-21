package com.resala.donation;

import com.resala.model.Donor;

public class BasicReceipt implements Receipt {

  private final Donation donation;
  private final Donor donor;

  public BasicReceipt(Donation donation, Donor donor) {
    this.donation = donation;
    this.donor = donor;
  }

  public String render() {
    String details = donation.getType().name();
    if (donation.getType().name().equals("IN_KIND")) {
      details += ": " + donation.getQuantity() + " x " + donation.getItemDescription();
    } else {
      details += ": " + money(donation.getAmountMinor());
    }
    return (
      "RESALA CHARITY\nReceipt #" +
      donation.getId() +
      "\nDate: " +
      donation.getDonationDate() +
      "\nDonor: " +
      donor.getName() +
      "\nDonation: " +
      details
    );
  }

  public static String money(long minor) {
    return String.format("%.2f EGP", minor / 100.0);
  }
}
