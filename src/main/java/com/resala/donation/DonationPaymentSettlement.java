package com.resala.donation;

import com.resala.model.DonationType;
import com.resala.model.Donor;
import com.resala.payment.Payment;
import com.resala.payment.PaymentSettlement;
import java.time.LocalDate;

/** Settles an approved payment by recording the matching online donation. */
public class DonationPaymentSettlement implements PaymentSettlement {

  private final DonationService donations;

  public DonationPaymentSettlement(DonationService donations) {
    this.donations = donations;
  }

  @Override
  public String referenceType() {
    return "DONATION";
  }

  @Override
  public long settle(Payment payment) {
    if (!"DONOR".equalsIgnoreCase(payment.getPayerType())) {
      throw new IllegalArgumentException("Payment is not for a donor");
    }
    DonationRequest request = new DonationRequest(
      payment.getPayerId(),
      DonationType.ONLINE,
      payment.getAmountMinor(),
      null,
      null,
      null,
      payment.getIdempotencyKey(),
      LocalDate.now(),
      payment.getNotes()
    );
    return donations.recordConfirmed(request).getId();
  }

  @Override
  public void afterCommit(Payment payment, long recordId) {
    Donation donation = donations.getDonation(recordId);
    Donor donor = donations.getDonor(payment.getPayerId());
    donations.notifyRecorded(donation, donor);
  }
}
