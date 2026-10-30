package com.resala;

import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.controller.PaymentController;
import com.resala.controller.VolunteerController;
import com.resala.core.DatabaseManager;
import com.resala.core.TransactionManager;
import com.resala.donation.DonationPaymentSettlement;
import com.resala.donation.DonationService;
import com.resala.event.EventService;
import com.resala.notification.EmailStrategy;
import com.resala.notification.EventNotificationObserver;
import com.resala.notification.Notifier;
import com.resala.notification.ThankYouNotificationObserver;
import com.resala.payment.FakePaymentGatewayAdapter;
import com.resala.payment.FakePaymentProvider;
import com.resala.payment.PaymentService;
import com.resala.persistence.sqlite.SqliteDonationRepository;
import com.resala.persistence.sqlite.SqliteDonorRepository;
import com.resala.persistence.sqlite.SqliteEventRegistrationRepository;
import com.resala.persistence.sqlite.SqliteEventRepository;
import com.resala.persistence.sqlite.SqlitePaymentRepository;
import com.resala.persistence.sqlite.SqliteVolunteerRepository;
import com.resala.view.UiFactory;
import com.resala.view.console.ConsoleUiFactory;
import com.resala.volunteer.VolunteerService;
import java.sql.Connection;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) {
    DatabaseManager database = DatabaseManager.getInstance();
    Connection conn = database.getConnection();

    // ── Shared Notification System (Strategy Pattern) ──────────────────────
    Notifier notifier = new Notifier(new EmailStrategy());

    // ── Donation module (Observer: new donation → thank you notification) ──
    SqlitePaymentRepository paymentRepository = new SqlitePaymentRepository(conn);
    DonationService donationService = new DonationService(
      new SqliteDonorRepository(conn),
      new SqliteDonationRepository(conn),
      paymentRepository
    );
    donationService.addObserver(new ThankYouNotificationObserver(notifier));
    DonationController donationController = new DonationController(donationService);

    // ── Online payments (gateway → settlement → donation) ───────────────────
    PaymentService paymentService = new PaymentService(
      paymentRepository,
      new FakePaymentGatewayAdapter(new FakePaymentProvider()),
      new TransactionManager(conn),
      new DonationPaymentSettlement(donationService)
    );
    PaymentController paymentController = new PaymentController(paymentService);

    // ── Event module (Observer: event update/close → attendee notification) ─
    SqliteEventRegistrationRepository eventRegistrationRepo = new SqliteEventRegistrationRepository(conn);
    EventService eventService = new EventService(
      new SqliteEventRepository(conn),
      eventRegistrationRepo
    );
    eventService.addObserver(new EventNotificationObserver(eventRegistrationRepo, notifier));
    EventController eventController = new EventController(eventService);

    // ── Volunteer module (Strategy: assignment rules) ───────────────────────
    VolunteerService volunteerService = new VolunteerService(
      new SqliteVolunteerRepository(conn),
      new SqliteEventRepository(conn)
    );
    VolunteerController volunteerController = new VolunteerController(volunteerService);

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
      System.out.println("3) Volunteer Management");
      System.out.println("0) Exit");
      System.out.println("========================================");
      System.out.print("Choose a module: ");
      String choice = input.nextLine().trim();

      switch (choice) {
        case "1" -> uiFactory.createDonationView(donationController, paymentController).start();
        case "2" -> uiFactory.createEventView(eventController).start();
        case "3" -> uiFactory.createVolunteerView(volunteerController).start();
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
