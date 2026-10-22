package com.resala.notification;

import com.resala.donation.Donation;
import com.resala.model.Donor;
import java.util.function.Consumer;

// mock for the real notification
public class ThankYouNotificationObserver implements DonationObserver {

  private final Consumer<String> notifier;

  public ThankYouNotificationObserver(Consumer<String> notifier) {
    this.notifier = notifier;
  }

  public void onDonationRecorded(Donation donation, Donor donor) {
    if (donor.getEmail() != null && !donor.getEmail().isBlank()) {
      notifier.accept("Thank-you notification queued for " + donor.getEmail());
    }
  }
}
