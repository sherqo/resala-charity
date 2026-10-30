package com.resala.view.console;

import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.controller.PaymentController;
import com.resala.controller.VolunteerController;
import com.resala.view.DonationView;
import com.resala.view.EventView;
import com.resala.view.UiFactory;
import com.resala.view.VolunteerView;
import java.util.Scanner;

// Concrete factory — produces the console family of views
public class ConsoleUiFactory implements UiFactory {

  // Single shared Scanner so all views read from the same stdin stream
  private final Scanner input = new Scanner(System.in);

  @Override
  public DonationView createDonationView(DonationController controller, PaymentController payments) {
    return new ConsoleDonationView(controller, payments, input);
  }

  @Override
  public EventView createEventView(EventController controller) {
    return new ConsoleEventView(controller, input);
  }

  @Override
  public VolunteerView createVolunteerView(VolunteerController controller) {
    return new ConsoleVolunteerView(controller, input);
  }
}
