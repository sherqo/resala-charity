package com.resala.view;

import com.resala.controller.BeneficiaryController;
import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.controller.PaymentController;
import com.resala.controller.VolunteerController;

// Abstract Factory pattern for the UI — adding a new module = add one method here
public interface UiFactory {
  DonationView    createDonationView(DonationController controller);
  EventView       createEventView(EventController controller);
  BeneficiaryView createBeneficiaryView(BeneficiaryController controller);
  DonationView createDonationView(DonationController controller, PaymentController payments);
  EventView    createEventView(EventController controller);
  VolunteerView createVolunteerView(VolunteerController controller);
}
