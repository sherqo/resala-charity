package com.resala.view;

import com.resala.controller.DonationController;

// Abstract Factory pattern for the UI stuff
public interface UiFactory {
  DonationView createDonationView(DonationController controller);
}
