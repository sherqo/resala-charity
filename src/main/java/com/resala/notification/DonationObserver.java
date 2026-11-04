package com.resala.notification;

import com.resala.donation.Donation;
import com.resala.model.Donor;

public interface DonationObserver {
  void onDonationRecorded(Donation donation, Donor donor);
}
