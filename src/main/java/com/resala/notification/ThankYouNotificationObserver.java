package com.resala.notification;

import com.resala.donation.Donation;
import com.resala.model.Donor;

/**
 * Observer — reacts to a recorded donation by sending a thank-you notification.
 * Delegates to whichever channel (Strategy) the Notifier currently holds,
 * so the communication channel can be swapped at runtime without touching this class.
 */
public class ThankYouNotificationObserver implements DonationObserver {

  private final Notifier notifier;

  public ThankYouNotificationObserver(Notifier notifier) {
    this.notifier = notifier;
  }

  @Override
  public void onDonationRecorded(Donation donation, Donor donor) {
    if (donor.getEmail() == null || donor.getEmail().isBlank()) return;

    String subject = "Thank you for your donation!";
    String body    = "Dear " + donor.getName() +
                     ", your donation #" + donation.getId() +
                     " has been recorded. We truly appreciate your generosity.";

    notifier.send(donor.getEmail(), subject, body);
  }
}
