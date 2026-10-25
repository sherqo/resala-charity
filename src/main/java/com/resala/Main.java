package com.resala;

import com.resala.beneficiary.BeneficiaryRepository;
import com.resala.beneficiary.BeneficiaryService;
import com.resala.controller.BeneficiaryController;
import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.core.DatabaseManager;
import com.resala.donation.DonationService;
import com.resala.event.EventService;
import com.resala.notification.EmailStrategy;
import com.resala.notification.EventNotificationObserver;
import com.resala.notification.Notifier;
import com.resala.notification.ThankYouNotificationObserver;
import com.resala.payment.FakePaymentGatewayAdapter;
import com.resala.payment.FakePaymentProvider;
import com.resala.persistence.sqlite.SqliteBeneficiaryRepository;
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
    Connection conn = database.getConnection();

    // ── Shared Notification System (Strategy Pattern) ──────────────────────
    Notifier notifier = new Notifier(new EmailStrategy());

    // ── Donation module (Observer: new donation → thank you notification) ──
    DonationService donationService = new DonationService(
      new SqliteDonorRepository(conn),
      new SqliteDonationRepository(conn),
      new FakePaymentGatewayAdapter(new FakePaymentProvider())
    );
    donationService.addObserver(new ThankYouNotificationObserver(notifier));
    DonationController donationController = new DonationController(donationService);

    // ── Event module (Observer: event update/close → attendee notification) ─
    SqliteEventRegistrationRepository eventRegistrationRepo = new SqliteEventRegistrationRepository(conn);
    EventService eventService = new EventService(
      new SqliteEventRepository(conn),
      eventRegistrationRepo
    );
    eventService.addObserver(new EventNotificationObserver(eventRegistrationRepo, notifier));
    EventController eventController = new EventController(eventService);

    // ── Beneficiary module ──────────────────────────────────────────────
    BeneficiaryRepository beneficiaryRepo = new SqliteBeneficiaryRepository(conn);
    BeneficiaryService beneficiaryService = new BeneficiaryService(beneficiaryRepo);
    BeneficiaryController beneficiaryController = new BeneficiaryController(beneficiaryService);
    
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
      System.out.println("3) Beneficiary Management");
      System.out.println("0) Exit");
      System.out.println("========================================");
      System.out.print("Choose a module: ");
      String choice = input.nextLine().trim();

      switch (choice) {
        case "1" -> uiFactory.createDonationView(donationController).start();
        case "2" -> uiFactory.createEventView(eventController).start();
        case "3" -> uiFactory.createBeneficiaryView(beneficiaryController).start();
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
