package com.resala.view.console;

import com.resala.controller.BeneficiaryController;
import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.view.BeneficiaryView;
import com.resala.view.DonationView;
import com.resala.view.EventView;
import com.resala.view.UiFactory;
import java.util.Scanner;

// Concrete factory — produces the console family of views
public class ConsoleUiFactory implements UiFactory {

  // Single shared Scanner so all views read from the same stdin stream
  private final Scanner input = new Scanner(System.in);

  @Override
  public DonationView createDonationView(DonationController controller) {
    return new ConsoleDonationView(controller, input);
  }

  @Override
  public EventView createEventView(EventController controller) {
    return new ConsoleEventView(controller, input);
  }

  @Override
  public BeneficiaryView createBeneficiaryView(BeneficiaryController controller) {
    return new ConsoleBeneficiaryView(controller, input);
  }
}
