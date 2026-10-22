package com.resala;

import com.resala.controller.DonationController;
import com.resala.core.DatabaseManager;
import com.resala.donation.DonationService;
import com.resala.notification.EmailStrategy;
import com.resala.notification.Notifier;
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

    // Observer + Strategy: default channel is Email; swap via notifier.setStrategy(...) at runtime
    Notifier notifier = new Notifier(new EmailStrategy());
    service.addObserver(new ThankYouNotificationObserver(notifier));

    DonationController controller = new DonationController(service);
    UiFactory uiFactory = new ConsoleUiFactory();
    uiFactory.createDonationView(controller).start();
    database.close();
  }
}
