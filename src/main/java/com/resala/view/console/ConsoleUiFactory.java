package com.resala.view.console;

import com.resala.controller.DonationController;
import com.resala.view.DonationView;
import com.resala.view.UiFactory;
import java.util.Scanner;

// concerete UI calss and this is what the main function choose to use in the compile time
public class ConsoleUiFactory implements UiFactory {

  public DonationView createDonationView(DonationController controller) {
    return new ConsoleDonationView(controller, new Scanner(System.in));
  }
}
