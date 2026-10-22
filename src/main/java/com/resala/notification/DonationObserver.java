package com.resala.notification;

import com.resala.donation.Donation;
import com.resala.model.Donor;

// Observer to receive a notification after a donation is stored
public interface DonationObserver {
  void onDonationRecorded(Donation donation, Donor donor);
}
