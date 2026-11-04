package com.resala.view.console;

import com.resala.controller.BeneficiaryController;
import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.view.BeneficiaryView;
import com.resala.controller.PaymentController;
import com.resala.controller.VolunteerController;
import com.resala.view.DonationView;
import com.resala.view.EventView;
import com.resala.view.UiFactory;
import com.resala.view.VolunteerView;
import java.util.Scanner;

public class ConsoleUiFactory implements UiFactory {

  private final Scanner input;

  public ConsoleUiFactory(Scanner input) {
    this.input = input;
  }

  @Override
  public DonationView createDonationView(DonationController controller, PaymentController payments) {
    return new ConsoleDonationView(controller, payments, input);
  }

  @Override
  public EventView createEventView(EventController controller) {
    return new ConsoleEventView(controller, input);
  }

  @Override
  public BeneficiaryView createBeneficiaryView(BeneficiaryController controller) {
    return new ConsoleBeneficiaryView(controller, input);
  }

  @Override
  public VolunteerView createVolunteerView(VolunteerController controller) {
    return new ConsoleVolunteerView(controller, input);
  }
}
