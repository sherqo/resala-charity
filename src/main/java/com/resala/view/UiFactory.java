package com.resala.view;

import com.resala.controller.BeneficiaryController;
import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.controller.PaymentController;
import com.resala.controller.VolunteerController;

public interface UiFactory {
  DonationView    createDonationView(DonationController controller, PaymentController payments);
  EventView       createEventView(EventController controller);
  BeneficiaryView createBeneficiaryView(BeneficiaryController controller);
  VolunteerView   createVolunteerView(VolunteerController controller);
}
