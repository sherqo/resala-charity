package com.resala.gui;

import com.resala.controller.BeneficiaryController;
import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.controller.PaymentController;
import com.resala.controller.VolunteerController;
import com.resala.gui.components.ConfirmDialog;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.Toast;
import com.resala.gui.components.Toolbar;
import com.resala.gui.views.*;
import com.resala.notification.DonationSubject;
import com.resala.notification.EventSubject;
import com.resala.notification.FeedObserver;
import com.resala.notification.Notifier;
import com.resala.payment.PaymentService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppShell {

    private final Stage stage;
    private final Map<String, BaseView> views = new LinkedHashMap<>();
    private final ListView<String> feedList = new ListView<>();
    private final Label statusLabel = new Label("Ready");
    private BaseView currentView;

    private final DonationController donationController;
    private final EventController eventController;
    private final VolunteerController volunteerController;
    private final PaymentController paymentController;
    private final PaymentService paymentService;
    private final BeneficiaryController beneficiaryController;
    private final Notifier notifier;

    private final DonationSubject donationSubject;
    private final EventSubject eventSubject;
    private final FeedObserver feedObserver;

    private final Toast toast = new Toast();
    private final ConfirmDialog confirmDialog = new ConfirmDialog();

    public AppShell(Stage stage,
                    DonationController donationController,
                    EventController eventController,
                    VolunteerController volunteerController,
                    PaymentController paymentController,
                    PaymentService paymentService,
                    DonationSubject donationSubject,
                    EventSubject eventSubject,
                    BeneficiaryController beneficiaryController,
                    Notifier notifier) {
        this.stage = stage;
        this.donationController = donationController;
        this.eventController = eventController;
        this.volunteerController = volunteerController;
        this.paymentController = paymentController;
        this.paymentService = paymentService;
        this.donationSubject = donationSubject;
        this.eventSubject = eventSubject;
        this.beneficiaryController = beneficiaryController;
        this.notifier = notifier;
        this.feedObserver = new FeedObserver(feedList, statusLabel);

        donationSubject.addObserver(feedObserver);
        eventSubject.addObserver(feedObserver);
    }

    private static AppShell instance;
    private StackPane rootPane;
    private BorderPane mainLayout;
    private StackPane modalOverlay;

    public static AppShell getInstance() { return instance; }
    public Stage getStage() { return stage; }
    public Toast getToast() { return toast; }
    public ConfirmDialog getConfirmDialog() { return confirmDialog; }
    public ListView<String> getFeedList() { return feedList; }
    public StackPane getModalOverlay() { return modalOverlay; }

    public void show() {
        instance = this;
        buildViews();
        Scene scene = new Scene(buildRoot(), 1280, 800);
        scene.setUserData(this);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setTitle("Resala Charity System");

        try {
            var icon256 = getClass().getResourceAsStream("/app-icon.png");
            if (icon256 != null) {
                stage.getIcons().add(new javafx.scene.image.Image(icon256));
            }
            var icon32 = getClass().getResourceAsStream("/app-icon-32.png");
            if (icon32 != null) {
                stage.getIcons().add(new javafx.scene.image.Image(icon32));
            }
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.show();

        showView("Dashboard");
    }

    private StackPane buildRoot() {
        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("root");
        mainLayout.setLeft(buildSidebar());
        mainLayout.setCenter(buildMainPane());
        mainLayout.setBottom(buildStatusBar());

        modalOverlay = new StackPane();
        modalOverlay.getStyleClass().add("modal-overlay");
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);

        rootPane = new StackPane(mainLayout, modalOverlay);
        rootPane.getStyleClass().add("root");
        return rootPane;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);
        sidebar.setMaxWidth(220);
        sidebar.setMinWidth(220);

        Label logo = new Label("Resala");
        logo.getStyleClass().add("logo");
        sidebar.getChildren().add(logo);

        VBox navBox = new VBox(2);
        navBox.setPadding(new Insets(12, 8, 12, 8));
        VBox.setVgrow(navBox, Priority.ALWAYS);

        List<NavItem> navItems = List.of(
            new NavItem("Dashboard"),
            new NavItem("Donations"),
            new NavItem("Payments"),
            new NavItem("Donors"),
            new NavItem("Volunteers"),
            new NavItem("Events"),
            new NavItem("Beneficiaries"),
            new NavItem("Messages"),
            new NavItem("Lookups"),
            new NavItem("Audit Log")
        );

        for (NavItem item : navItems) {
            Button btn = new Button(item.name);
            btn.getStyleClass().add("nav-btn");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> showView(item.name));
            item.button = btn;
            navBox.getChildren().add(btn);
        }

        sidebar.getChildren().add(navBox);

        VBox footerBox = new VBox(6);
        footerBox.setPadding(new Insets(8, 8, 12, 8));

        Button themeBtn = new Button("Dark Theme");
        themeBtn.getStyleClass().addAll("btn", "btn-ghost");
        themeBtn.setMaxWidth(Double.MAX_VALUE);
        themeBtn.setOnAction(e -> {
            Scene scene = stage.getScene();
            if (scene != null && scene.getRoot() != null) {
                boolean isDark = scene.getRoot().getStyleClass().contains("dark-theme");
                if (isDark) {
                    scene.getRoot().getStyleClass().remove("dark-theme");
                    themeBtn.setText("Dark Theme");
                } else {
                    scene.getRoot().getStyleClass().add("dark-theme");
                    themeBtn.setText("Light Theme");
                }
            }
        });

        footerBox.getChildren().add(themeBtn);
        sidebar.getChildren().add(footerBox);

        this.navItems = navItems;

        return sidebar;
    }

    private List<NavItem> navItems;

    private VBox buildMainPane() {
        VBox mainPane = new VBox();
        mainPane.getStyleClass().add("main-pane");

        headerBar = new HeaderBar();
        mainPane.getChildren().add(headerBar);

        contentStack = new StackPane();
        contentStack.getStyleClass().add("content-area");
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        for (BaseView view : views.values()) {
            view.setVisible(false);
            view.setManaged(false);
            contentStack.getChildren().add(view);
        }

        mainPane.getChildren().add(contentStack);
        return mainPane;
    }

    private HeaderBar headerBar;
    private StackPane contentStack;

    private HBox buildStatusBar() {
        HBox statusBar = new HBox(12);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel.getStyleClass().add("status-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label systemStatus = new Label("Connected");
        systemStatus.getStyleClass().add("status-label");

        statusBar.getChildren().addAll(statusLabel, spacer, systemStatus);
        return statusBar;
    }

    private void buildViews() {

        DashboardView dashboard = new DashboardView(this, donationController, eventController, volunteerController);
        donationSubject.addObserver(dashboard);
        eventSubject.addObserver(dashboard);
        views.put("Dashboard", dashboard);

        DonationView donationView = new DonationView(this, donationController, paymentController);
        donationSubject.addObserver(donationView);
        views.put("Donations", donationView);

        PaymentView paymentView = new PaymentView(this, paymentController);
        views.put("Payments", paymentView);

        views.put("Donors", new DonorView(this, donationController));

        views.put("Volunteers", new VolunteerView(this, volunteerController));

        EventView eventView = new EventView(this, eventController);
        eventSubject.addObserver(eventView);
        views.put("Events", eventView);

        views.put("Beneficiaries", new BeneficiaryView(this, beneficiaryController));

        views.put("Messages", new CommView(this, notifier));

        views.put("Lookups", new LookupView(this, volunteerController, eventController));

        views.put("Audit Log", new AuditLogView(this));
    }

    public void refreshAll() {
        javafx.application.Platform.runLater(() -> {
            for (BaseView view : views.values()) {
                view.refresh();
            }
        });
    }

    public void showView(String name) {
        BaseView view = views.get(name);
        if (view == null) return;

        if (currentView != null) {
            currentView.setVisible(false);
            currentView.setManaged(false);
        }

        view.setVisible(true);
        view.setManaged(true);
        currentView = view;

        for (NavItem item : navItems) {
            item.button.getStyleClass().remove("active");
            if (item.name.equals(name)) {
                item.button.getStyleClass().add("active");
            }
        }

        headerBar.setTitle(name);
        headerBar.setSubtitle(getSubtitle(name));
        headerBar.setPrimaryAction(view.getPrimaryAction());

        view.refresh();
    }

    private String getSubtitle(String viewName) {
        return switch (viewName) {
            case "Dashboard" -> "Overview & activity feed";
            case "Donations" -> "Manage donations & receipts";
            case "Donors" -> "Donor directory";
            case "Volunteers" -> "Volunteer management & certificates";
            case "Events" -> "Event registration & attendance";
            case "Beneficiaries" -> "Beneficiary aid & impact tracking";
            case "Messages" -> "Multi-channel notifications (Strategy)";
            case "Lookups" -> "Admin lookup tables";
            default -> "";
        };
    }

    private static class NavItem {
        final String name;
        Button button;

        NavItem(String name) {
            this.name = name;
        }
    }

    public static class HeaderBar extends HBox {
        private final Label title = new Label();
        private final Label subtitle = new Label();
        private final Button primaryAction = new Button();

        public HeaderBar() {
            getStyleClass().add("header-bar");
            setPadding(new Insets(16, 24, 16, 24));
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(16);

            title.getStyleClass().add("header-title");
            subtitle.getStyleClass().add("header-subtitle");
            primaryAction.getStyleClass().addAll("btn", "btn-primary");
            primaryAction.setVisible(false);
            primaryAction.setManaged(false);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            getChildren().addAll(title, subtitle, spacer, primaryAction);
        }

        public void setTitle(String title) {
            this.title.setText(title);
        }

        public void setSubtitle(String subtitle) {
            this.subtitle.setText(subtitle);
            this.subtitle.setVisible(!subtitle.isBlank());
            this.subtitle.setManaged(!subtitle.isBlank());
        }

        public void setPrimaryAction(Button button) {
            if (button != null) {
                primaryAction.setText(button.getText());
                primaryAction.setOnAction(button.getOnAction());
                primaryAction.getStyleClass().setAll("btn", "btn-primary");
                primaryAction.setVisible(true);
                primaryAction.setManaged(true);
            } else {
                primaryAction.setVisible(false);
                primaryAction.setManaged(false);
            }
        }
    }
}
