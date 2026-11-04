package com.resala.gui.views;

import com.resala.controller.DonationController;
import com.resala.controller.EventController;
import com.resala.controller.VolunteerController;
import com.resala.donation.Donation;
import com.resala.donation.DonationFilter;
import com.resala.event.EventService;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.StatCard;
import com.resala.gui.components.Toolbar;
import com.resala.model.Donor;
import com.resala.model.Event;
import com.resala.model.Volunteer;
import com.resala.notification.DonationObserver;
import com.resala.notification.EventObserver;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class DashboardView extends BaseView implements DonationObserver, EventObserver {

    private final AppShell appShell;
    private final DonationController donationController;
    private final EventController eventController;
    private final VolunteerController volunteerController;

    private final VBox totalDonationsCard;
    private final VBox totalDonorsCard;
    private final VBox totalVolunteersCard;
    private final VBox totalEventsCard;
    private final VBox upcomingEventsCard;

    private final DataTable<Donation> recentDonationsTable;
    private final ListView<String> feedList;

    private Button primaryActionButton;

    @SuppressWarnings("unchecked")
    public DashboardView(AppShell appShell,
                         DonationController donationController,
                         EventController eventController,
                         VolunteerController volunteerController) {
        super(createToolbar(), createRecentDonationsTable());

        this.appShell = appShell;
        this.donationController = donationController;
        this.eventController = eventController;
        this.volunteerController = volunteerController;
        this.recentDonationsTable = (DataTable<Donation>) this.table;

        this.totalDonationsCard = StatCard.of("Total Donations", "0");
        this.totalDonorsCard = StatCard.of("Total Donors", "0");
        this.totalVolunteersCard = StatCard.of("Total Volunteers", "0");
        this.totalEventsCard = StatCard.of("Total Events", "0");
        this.upcomingEventsCard = StatCard.of("Upcoming Events", "0");

        this.feedList = appShell.getFeedList();
        feedList.getStyleClass().add("feed-list-view");
        feedList.setPrefHeight(180);

        buildLayout();

        this.primaryActionButton = Toolbar.primary("Add Donation", e -> appShell.showView("Donations"));

        refresh();
    }

    @Override
    public void onDonationRecorded(Donation donation, Donor donor) {
        Platform.runLater(this::refresh);
    }

    @Override
    public void onEventUpdated(Event event, String message) {
        Platform.runLater(this::refresh);
    }

    @Override
    public Button getPrimaryAction() {
        return primaryActionButton;
    }

    private static HBox createToolbar() {
        return Toolbar.of();
    }

    private static DataTable<Donation> createRecentDonationsTable() {
        return DataTable.<Donation>builder()
            .column("ID", 60, (java.util.function.Function<Donation, String>) d -> String.valueOf(d.getId()))
            .column("Donor", 180, (java.util.function.Function<Donation, String>) d -> "Donor #" + d.getDonorId())
            .column("Amount", 100, (java.util.function.Function<Donation, String>) d -> formatAmount(d))
            .column("Type", 100, (java.util.function.Function<Donation, String>) d -> d.getType().name())
            .column("Date", 120, (java.util.function.Function<Donation, String>) d -> d.getDonationDate().toString())
            .items(FXCollections.observableArrayList())
            .build();
    }

    private void buildLayout() {

        GridPane statGrid = new GridPane();
        statGrid.getStyleClass().add("stat-grid");
        statGrid.setHgap(16);
        statGrid.setVgap(16);
        statGrid.add(totalDonationsCard, 0, 0);
        statGrid.add(totalDonorsCard, 1, 0);
        statGrid.add(totalVolunteersCard, 2, 0);
        statGrid.add(totalEventsCard, 3, 0);
        statGrid.add(upcomingEventsCard, 4, 0);

        VBox recentSection = new VBox(8);
        Label recentTitle = new Label("Recent Donations");
        recentTitle.getStyleClass().add("card-title");
        recentSection.getChildren().addAll(recentTitle, getTableView());

        VBox feedSection = new VBox(8);
        Label feedTitle = new Label("Activity Feed");
        feedTitle.getStyleClass().add("card-title");
        feedSection.getChildren().addAll(feedTitle, feedList);

        VBox content = new VBox(24);
        content.setPadding(new Insets(0, 0, 24, 0));
        content.getChildren().addAll(statGrid, recentSection, feedSection);

        getChildren().clear();
        getChildren().add(content);
    }

    @Override
    public void refresh() {
        List<Donation> donations = donationController.findDonations(new DonationFilter(null, null, null, null));
        List<Donor> donors = donationController.findDonors("");
        List<Volunteer> volunteers = volunteerController.listVolunteers();
        List<Event> events = eventController.listEvents();

        StatCard.valueLabel(totalDonationsCard).setText(String.valueOf(donations.size()));
        StatCard.valueLabel(totalDonorsCard).setText(String.valueOf(donors.size()));
        StatCard.valueLabel(totalVolunteersCard).setText(String.valueOf(volunteers.size()));
        StatCard.valueLabel(totalEventsCard).setText(String.valueOf(events.size()));

        long upcoming = events.stream()
            .filter(e -> e.getStatus() == Event.Status.OPEN || e.getStatus() == Event.Status.DRAFT)
            .count();
        StatCard.valueLabel(upcomingEventsCard).setText(String.valueOf(upcoming));

        List<Donation> recent = donations.stream()
            .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
            .limit(5)
            .toList();

        recentDonationsTable.getTableView().setItems(FXCollections.observableArrayList(recent));
    }

    private static String formatAmount(Donation d) {
        if (d.getAmountMinor() > 0) {
            return String.format("%,.2f EGP", d.getAmountMinor() / 100.0);
        } else if (d.getEstimatedValueMinor() != null && d.getEstimatedValueMinor() > 0) {
            return String.format("%,.2f EGP (est.)", d.getEstimatedValueMinor() / 100.0);
        }
        return "—";
    }
}
