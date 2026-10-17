# Resala Charity System: Filesystem (Java, Maven)

Package root: `com.resala`. Pattern files are tagged with the owner (P1-P5) and phase.

```
resala-charity/
├── pom.xml
├── README.md
├── plans.md
├── files.md
├── .gitignore
├── docs/
│   ├── class-diagram.png            (P5)
│   ├── client-requirements.md       (P5)
│   └── screenshots/                 (P5)
├── data/
│   └── resala.db                    (SQLite file, git-ignored)
├── src/
│   ├── main/
│   │   ├── java/com/resala/
│   │   │   ├── Main.java                         (P1) entry point, console or --gui
│   │   │   │
│   │   │   ├── model/                            (shared, agreed Day 1)
│   │   │   │   ├── Donor.java
│   │   │   │   ├── Volunteer.java
│   │   │   │   ├── Event.java
│   │   │   │   ├── Beneficiary.java
│   │   │   │   ├── User.java
│   │   │   │   └── Role.java                     enum: ADMIN, STAFF, VOLUNTEER
│   │   │   │
│   │   │   ├── core/                             (P1)
│   │   │   │   ├── DatabaseManager.java          Singleton (Phase 1)
│   │   │   │   ├── ConfigManager.java            Singleton (Phase 1)
│   │   │   │   ├── NonProfitFacade.java          Facade (Phase 2)
│   │   │   │   ├── AuthService.java
│   │   │   │   └── command/                      Command (Phase 2)
│   │   │   │       ├── Command.java
│   │   │   │       ├── CommandHistory.java
│   │   │   │       └── AddDonationCommand.java
│   │   │   │
│   │   │   ├── security/                         (P1) Proxy (Phase 2)
│   │   │   │   ├── AdminService.java
│   │   │   │   ├── AdminServiceImpl.java
│   │   │   │   └── AdminServiceProxy.java
│   │   │   │
│   │   │   ├── donation/                         (P2)
│   │   │   │   ├── Donation.java                 interface
│   │   │   │   ├── OnlineDonation.java
│   │   │   │   ├── CheckDonation.java
│   │   │   │   ├── InKindDonation.java
│   │   │   │   ├── DonationFactory.java          Factory (Phase 1)
│   │   │   │   ├── decorator/                    Decorator (Phase 1)
│   │   │   │   │   ├── DonationDecorator.java
│   │   │   │   │   ├── TaxReceiptDecorator.java
│   │   │   │   │   └── ThankYouDecorator.java
│   │   │   │   ├── payment/                      Adapter (Phase 2)
│   │   │   │   │   ├── PaymentProcessor.java
│   │   │   │   │   ├── LegacyGateway.java
│   │   │   │   │   └── FakeGatewayAdapter.java
│   │   │   │   ├── report/                       Template Method (Phase 2)
│   │   │   │   │   ├── ReportGenerator.java
│   │   │   │   │   ├── ReceiptReport.java
│   │   │   │   │   └── MonthlyReport.java
│   │   │   │   ├── DonationRepository.java
│   │   │   │   └── DonationService.java
│   │   │   │
│   │   │   ├── volunteer/                        (P3)
│   │   │   │   ├── VolunteerRepository.java
│   │   │   │   ├── VolunteerService.java
│   │   │   │   ├── VolunteerCollection.java      Iterator (Phase 2)
│   │   │   │   └── CertificateGenerator.java
│   │   │   │
│   │   │   ├── event/                            (P3)
│   │   │   │   ├── EventRepository.java
│   │   │   │   ├── EventService.java
│   │   │   │   └── state/                        State (Phase 2)
│   │   │   │       ├── EventState.java
│   │   │   │       ├── DraftState.java
│   │   │   │       ├── OpenState.java
│   │   │   │       └── ClosedState.java
│   │   │   │
│   │   │   ├── beneficiary/                      (P4)
│   │   │   │   ├── BeneficiaryRepository.java
│   │   │   │   ├── BeneficiaryService.java
│   │   │   │   └── DistributionService.java
│   │   │   │
│   │   │   ├── notification/                     (P4)
│   │   │   │   ├── NotificationStrategy.java     Strategy (Phase 1)
│   │   │   │   ├── EmailStrategy.java
│   │   │   │   ├── SmsStrategy.java
│   │   │   │   ├── SocialStrategy.java
│   │   │   │   ├── Notifier.java
│   │   │   │   ├── Observer.java                 Observer (Phase 1)
│   │   │   │   └── Subject.java
│   │   │   │
│   │   │   ├── controller/                       (each owner for their module)
│   │   │   │   ├── DonationController.java       P2
│   │   │   │   ├── VolunteerController.java      P3
│   │   │   │   ├── EventController.java          P3
│   │   │   │   ├── BeneficiaryController.java    P4
│   │   │   │   ├── AdminController.java          P5
│   │   │   │   └── AuthController.java           P1
│   │   │   │
│   │   │   └── view/                             (P5, console)
│   │   │       ├── ConsoleMenu.java
│   │   │       ├── DonationView.java
│   │   │       ├── VolunteerView.java
│   │   │       ├── EventView.java
│   │   │       ├── BeneficiaryView.java
│   │   │       ├── AdminView.java
│   │   │       └── ControlPanelView.java
│   │   │
│   │   └── resources/
│   │       ├── schema.sql                        (P1)
│   │       ├── seed.sql                          (P1) sample data for demo
│   │       └── config.properties
│   │
│   └── test/java/com/resala/
│       ├── donation/
│       │   ├── DonationFactoryTest.java
│       │   └── DecoratorTest.java
│       ├── notification/
│       │   ├── StrategyTest.java
│       │   └── ObserverTest.java
│       ├── core/
│       │   └── SingletonTest.java
│       └── event/
│           └── EventStateTest.java
```

## Optional JavaFX additions (only if time allows)

```
src/main/java/com/resala/gui/
├── GuiApp.java                    (P1) extends Application, launched by Main --gui
├── SceneManager.java              (P1) switchTo("name")
├── controller/                    FX controllers, call NonProfitFacade only
│   ├── LoginFxController.java         P1
│   ├── DashboardFxController.java     P5
│   ├── DonationFxController.java      P2
│   ├── VolunteerFxController.java     P3
│   ├── EventFxController.java         P3
│   ├── BeneficiaryFxController.java   P4
│   ├── NotificationFxController.java  P4
│   └── AdminFxController.java         P5
└── util/
    └── AlertHelper.java

src/main/resources/gui/
├── style.css                      (P5)
└── fxml/
    ├── login.fxml
    ├── dashboard.fxml
    ├── donations.fxml
    ├── volunteers.fxml
    ├── events.fxml
    ├── beneficiaries.fxml
    ├── notifications.fxml
    └── admin.fxml
```

Add to `pom.xml`: `javafx-controls`, `javafx-fxml`, and `javafx-maven-plugin`. If the lecturer forbids Maven, use plain `--module-path` flags instead.

## Conventions

- **Package by feature**, not by layer, except `controller/` and `view/`, which stay central so Person 5 can see the whole UI.
- **Naming:** `*Service` = business logic, `*Repository` = SQL/storage, `*Controller` = input handling, `*View` = console output.
- **No cross-module imports of implementations.** Import only the other module's `*Service` interface.
- **Branches:** `main` (protected, Person 1 merges), `feature/p2-donations`, `feature/p3-events`, etc.
- **Commit often**, small commits, message format `module: what changed`.
