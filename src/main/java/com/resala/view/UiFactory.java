package com.resala.view;

import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.controller.PaymentController;

// Abstract Factory pattern for the UI — adding a new module = add one method here
public interface UiFactory {
  DonationView createDonationView(DonationController controller, PaymentController payments);
  EventView    createEventView(EventController controller);
}
