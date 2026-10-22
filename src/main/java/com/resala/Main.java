package com.resala;

import com.resala.controller.DonationController;
import com.resala.core.DatabaseManager;
import com.resala.donation.DonationService;
import com.resala.notification.ThankYouNotificationObserver;
import com.resala.payment.FakePaymentGatewayAdapter;
import com.resala.payment.FakePaymentProvider;
import com.resala.persistence.sqlite.SqliteDonationRepository;
import com.resala.persistence.sqlite.SqliteDonorRepository;
import com.resala.view.UiFactory;
import com.resala.view.console.ConsoleUiFactory;

public class Main {

  public static void main(String[] args) {
    DatabaseManager database = DatabaseManager.getInstance();
    DonationService service = new DonationService(
      new SqliteDonorRepository(database.getConnection()),
      new SqliteDonationRepository(database.getConnection()),
      new FakePaymentGatewayAdapter(new FakePaymentProvider())
    );
    service.addObserver(new ThankYouNotificationObserver(System.out::println));

    DonationController controller = new DonationController(service);
    UiFactory uiFactory = new ConsoleUiFactory();
    uiFactory.createDonationView(controller).start();
    database.close();
  }
}
