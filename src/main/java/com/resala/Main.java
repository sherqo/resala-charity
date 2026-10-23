package com.resala;

import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.core.DatabaseManager;
import com.resala.donation.DonationService;
import com.resala.notification.EmailStrategy;
import com.resala.notification.Notifier;
import com.resala.notification.ThankYouNotificationObserver;
import com.resala.payment.FakePaymentGatewayAdapter;
import com.resala.payment.FakePaymentProvider;
import com.resala.persistence.sqlite.SqliteDonationRepository;
import com.resala.persistence.sqlite.SqliteDonorRepository;
import com.resala.persistence.sqlite.SqliteEventRegistrationRepository;
import com.resala.persistence.sqlite.SqliteEventRepository;
import com.resala.view.UiFactory;
import com.resala.view.console.ConsoleUiFactory;
import java.sql.Connection;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) {
    DatabaseManager database = DatabaseManager.getInstance();

    DonationService service = new DonationService(
      new SqliteDonorRepository(database.getConnection()),
      new SqliteDonationRepository(database.getConnection()),
      new FakePaymentGatewayAdapter(new FakePaymentProvider())
    );

    // Observer + Strategy: default channel is Email; swap via notifier.setStrategy(...) at runtime
    Notifier notifier = new Notifier(new EmailStrategy());
    service.addObserver(new ThankYouNotificationObserver(notifier));

    // ── Top-level module menu ──────────────────────────────────────────────
    UiFactory uiFactory = new ConsoleUiFactory();
    Scanner input = new Scanner(System.in);
    boolean running = true;
    System.out.println("Welcome to Resala Charity System");

    while (running) {
      System.out.println("========================================");
      System.out.println("  Main Menu");
      System.out.println("========================================");
      System.out.println("1) Donation Management");
      System.out.println("2) Event Management");
      System.out.println("0) Exit");
      System.out.println("========================================");
      System.out.print("Choose a module: ");
      String choice = input.nextLine().trim();

      switch (choice) {
        case "1" -> uiFactory.createDonationView(donationController).start();
        case "2" -> uiFactory.createEventView(eventController).start();
        case "0" -> {
          System.out.println("Goodbye.");
          running = false;
        }
        default  -> System.out.println("Invalid option.");
      }
    }

    database.close();
  }
}
