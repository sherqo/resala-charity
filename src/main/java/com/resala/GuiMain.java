package com.resala;

import com.resala.beneficiary.BeneficiaryService;
import com.resala.controller.BeneficiaryController;
import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.controller.PaymentController;
import com.resala.controller.VolunteerController;
import com.resala.core.DatabaseManager;
import com.resala.core.TransactionManager;
import com.resala.donation.DonationPaymentSettlement;
import com.resala.donation.DonationService;
import com.resala.event.EventService;
import com.resala.event.EventTypeService;
import com.resala.persistence.sqlite.SqliteBeneficiaryRepository;
import com.resala.persistence.sqlite.SqliteEventTypeRepository;
import com.resala.model.EventType;
import com.resala.gui.AppShell;
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
import com.resala.view.JavaFxUiFactory;
import com.resala.volunteer.VolunteerService;
import javafx.application.Application;
import javafx.stage.Stage;
import java.sql.Connection;

/**
 * JavaFX GUI entry point.
 * Run with: mvn javafx:run
 */
public class GuiMain extends Application {

    @Override
    public void start(Stage stage) {
        DatabaseManager database = DatabaseManager.getInstance();
        Connection conn = database.getConnection();

        // Shared Notification System (Strategy Pattern)
        Notifier notifier = new Notifier(new EmailStrategy());

        // Donation module (Observer: new donation → thank you notification)
        SqlitePaymentRepository paymentRepository = new SqlitePaymentRepository(conn);
        DonationService donationService = new DonationService(
            new SqliteDonorRepository(conn),
            new SqliteDonationRepository(conn),
            paymentRepository
        );
        donationService.addObserver(new ThankYouNotificationObserver(notifier));
        DonationController donationController = new DonationController(donationService);

        // Online payments (gateway → settlement → donation)
        PaymentService paymentService = new PaymentService(
            paymentRepository,
            new FakePaymentGatewayAdapter(new FakePaymentProvider()),
            new TransactionManager(conn),
            new DonationPaymentSettlement(donationService)
        );
        PaymentController paymentController = new PaymentController(paymentService);

        // Event module (Observer: event update/close → attendee notification)
        SqliteEventRegistrationRepository eventRegistrationRepo = new SqliteEventRegistrationRepository(conn);
        EventService eventService = new EventService(
            new SqliteEventRepository(conn),
            eventRegistrationRepo
        );
        eventService.addObserver(new EventNotificationObserver(eventRegistrationRepo, notifier));
        EventTypeService eventTypeService = new EventTypeService(new SqliteEventTypeRepository(conn));
        EventController eventController = new EventController(eventService, eventTypeService);

        // Volunteer module
        VolunteerService volunteerService = new VolunteerService(
            new SqliteVolunteerRepository(conn),
            new SqliteEventRepository(conn)
        );
        VolunteerController volunteerController = new VolunteerController(volunteerService);

        // Beneficiary module
        BeneficiaryService beneficiaryService = new BeneficiaryService(new SqliteBeneficiaryRepository(conn));
        BeneficiaryController beneficiaryController = new BeneficiaryController(beneficiaryService);

        // Create JavaFX UI Factory (Abstract Factory pattern)
        JavaFxUiFactory uiFactory = new JavaFxUiFactory(
            donationController, paymentController,
            eventController, volunteerController,
            donationService, eventService
        );

        // Launch AppShell
        new AppShell(stage,
            donationController, eventController, volunteerController,
            paymentController, paymentService,
            donationService, eventService,
            beneficiaryController
        ).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}