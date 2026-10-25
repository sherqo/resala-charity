package com.resala.view;

import com.resala.controller.BeneficiaryController;
import com.resala.controller.DonationController;
import com.resala.controller.EventController;

// Abstract Factory pattern for the UI — adding a new module = add one method here
public interface UiFactory {
  DonationView    createDonationView(DonationController controller);
  EventView       createEventView(EventController controller);
  BeneficiaryView createBeneficiaryView(BeneficiaryController controller);
}
