package com.resala.gui;

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

/**
 * Main application shell - replaces MainShell.
 * Owns the stage, sidebar navigation, header bar, content area (StackPane), and status bar.
 * Constructs all views once at startup and switches visibility.
 */
public class AppShell {

    private final Stage stage;
    private final Map<String, BaseView> views = new LinkedHashMap<>();
    private final ListView<String> feedList = new ListView<>();
    private final Label statusLabel = new Label("Ready");
    private BaseView currentView;

    // Controllers
    private final DonationController donationController;
    private final EventController eventController;
    private final VolunteerController volunteerController;
    private final PaymentController paymentController;
    private final PaymentService paymentService;

    // Observer subjects
    private final DonationSubject donationSubject;
    private final EventSubject eventSubject;
    private final FeedObserver feedObserver;

    // Shared UI components
    private final Toast toast = new Toast();
    private final ConfirmDialog confirmDialog = new ConfirmDialog();

    public AppShell(Stage stage,
                    DonationController donationController,
                    EventController eventController,
                    VolunteerController volunteerController,
                    PaymentController paymentController,
                    PaymentService paymentService,
                    DonationSubject donationSubject,
                    EventSubject eventSubject) {
        this.stage = stage;
        this.donationController = donationController;
        this.eventController = eventController;
        this.volunteerController = volunteerController;
        this.paymentController = paymentController;
        this.paymentService = paymentService;
        this.donationSubject = donationSubject;
        this.eventSubject = eventSubject;
        this.feedObserver = new FeedObserver(feedList, statusLabel);

        // Attach feed observer
        donationSubject.addObserver(feedObserver);
        eventSubject.addObserver(feedObserver);
    }

    // Helper methods for views
    public Stage getStage() { return stage; }
    public Toast getToast() { return toast; }
    public ConfirmDialog getConfirmDialog() { return confirmDialog; }

    public void show() {
        buildViews();
        Scene scene = new Scene(buildRoot(), 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setTitle("Resala Charity System");
        stage.setScene(scene);
        stage.show();

        // Show dashboard by default
        showView("Dashboard");
    }

    private BorderPane buildRoot() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setLeft(buildSidebar());
        root.setCenter(buildMainPane());
        root.setBottom(buildStatusBar());
        return root;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(260);
        sidebar.setMaxWidth(260);
        sidebar.setMinWidth(260);

        // Logo/Title
        Label logo = new Label("🏛 Resala");
        logo.getStyleClass().add("logo");
        sidebar.getChildren().add(logo);

        // Navigation buttons
        VBox navBox = new VBox(4);
        navBox.setPadding(new Insets(16));
        VBox.setVgrow(navBox, Priority.ALWAYS);

        List<NavItem> navItems = List.of(
            new NavItem("Dashboard", "📊", DashboardView.class),
            new NavItem("Donations", "💝", DonationView.class),
            new NavItem("Donors", "👥", DonorView.class),
            new NavItem("Volunteers", "🤝", VolunteerView.class),
            new NavItem("Events", "📅", EventView.class),
            new NavItem("Beneficiaries", "🎁", BeneficiaryView.class),
            new NavItem("Messages", "💬", CommView.class),
            new NavItem("Lookups", "🔧", LookupView.class)
        );

        for (NavItem item : navItems) {
            Button btn = new Button(item.icon + "  " + item.name);
            btn.getStyleClass().add("nav-btn");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> showView(item.name));
            item.button = btn;
            navBox.getChildren().add(btn);
        }

        sidebar.getChildren().add(navBox);

        // Version label at bottom
        Label version = new Label("v1.0.0");
        version.getStyleClass().add("version");
        version.setPadding(new Insets(16));
        sidebar.getChildren().add(version);

        // Store nav items for active state management
        this.navItems = navItems;

        return sidebar;
    }

    private List<NavItem> navItems;

    private VBox buildMainPane() {
        VBox mainPane = new VBox();
        mainPane.getStyleClass().add("main-pane");

        // Header bar
        headerBar = new HeaderBar();
        mainPane.getChildren().add(headerBar);

        // Content area - StackPane for view switching
        contentStack = new StackPane();
        contentStack.getStyleClass().add("content-area");
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        // Add all views to stack (hidden initially)
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
        HBox statusBar = new HBox(16);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(8, 16, 8, 16));
        statusBar.setAlignment(Pos.CENTER_LEFT);

        // Feed summary on left
        Label feedTitle = new Label("Activity Feed:");
        feedTitle.getStyleClass().add("status-label");
        feedList.setPrefHeight(60);
        feedList.setMaxHeight(60);
        feedList.getStyleClass().add("feed-list");

        VBox feedBox = new VBox(4, feedTitle, feedList);
        HBox.setHgrow(feedBox, Priority.ALWAYS);

        // Status label on right
        statusLabel.getStyleClass().add("status-text");

        statusBar.getChildren().addAll(feedBox, statusLabel);
        return statusBar;
    }

    private void buildViews() {
        // Dashboard
        views.put("Dashboard", new DashboardView(this, donationController, eventController, volunteerController));

        // Donations
        views.put("Donations", new DonationView(this, donationController, paymentController, paymentService));

        // Donors
        views.put("Donors", new DonorView(this, donationController));

        // Volunteers
        views.put("Volunteers", new VolunteerView(this, volunteerController));

        // Events
        views.put("Events", new EventView(this, eventController));

        // Beneficiaries
        views.put("Beneficiaries", new BeneficiaryView(this));

        // Messages
        views.put("Messages", new CommView(this));

        // Lookups
        views.put("Lookups", new LookupView(this, volunteerController, eventController));
    }

    public void showView(String name) {
        BaseView view = views.get(name);
        if (view == null) return;

        // Hide current view
        if (currentView != null) {
            currentView.setVisible(false);
            currentView.setManaged(false);
        }

        // Show new view
        view.setVisible(true);
        view.setManaged(true);
        currentView = view;

        // Update nav button active state
        for (NavItem item : navItems) {
            item.button.getStyleClass().remove("active");
            if (item.name.equals(name)) {
                item.button.getStyleClass().add("active");
            }
        }

        // Update header
        headerBar.setTitle(name);
        headerBar.setSubtitle(getSubtitle(name));
        headerBar.setPrimaryAction(view.getPrimaryAction());

        // Refresh view data
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

    // Getters for views to access controllers
    public DonationController getDonationController() { return donationController; }
    public EventController getEventController() { return eventController; }
    public VolunteerController getVolunteerController() { return volunteerController; }
    public PaymentController getPaymentController() { return paymentController; }
    public PaymentService getPaymentService() { return paymentService; }

    private static class NavItem {
        final String name;
        final String icon;
        final Class<? extends BaseView> viewClass;
        Button button;

        NavItem(String name, String icon, Class<? extends BaseView> viewClass) {
            this.name = name;
            this.icon = icon;
            this.viewClass = viewClass;
        }
    }

    /**
     * Header bar shown at top of content area.
     */
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