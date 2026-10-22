package com.resala.notification;

public interface DonationSubject {
  void addObserver(DonationObserver observer);
  void removeObserver(DonationObserver observer);
}
