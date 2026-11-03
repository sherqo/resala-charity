package com.resala.notification;

import com.resala.donation.Donation;
import com.resala.model.Donor;
import com.resala.model.Event;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FeedObserver implements DonationObserver, EventObserver {

    private final ListView<String> feedList;
    private final Label statusLabel;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public FeedObserver(ListView<String> feedList, Label statusLabel) {
        this.feedList = feedList;
        this.statusLabel = statusLabel;
    }

    @Override
    public void onDonationRecorded(Donation donation, Donor donor) {
        String time = LocalDateTime.now().format(timeFormatter);
        String amount = donation.getAmountMinor() > 0
            ? String.format(" — %,.2f EGP", donation.getAmountMinor() / 100.0)
            : "";
        String item = donation.getItemDescription() != null
            ? " (" + donation.getItemDescription() + ")"
            : "";
        String entry = String.format("[%s] New %s donation from %s%s%s",
            time, donation.getType().name().toLowerCase(), donor.getName(), amount, item);
        addToFeed(entry);
        statusLabel.setText("Last: Donation from " + donor.getName() + " at " + time);
    }

    @Override
    public void onEventUpdated(Event event, String message) {
        String time = LocalDateTime.now().format(timeFormatter);
        String entry = String.format("[%s] Event: %s — %s", time, event.getTitle(), message);
        addToFeed(entry);
        statusLabel.setText("Last: Event update - " + event.getTitle() + " at " + time);
    }

    private void addToFeed(String entry) {
        // Add to top of list (most recent first)
        ObservableList<String> items = feedList.getItems();
        items.add(0, entry);
        // Keep only last 50 entries
        while (items.size() > 50) {
            items.remove(items.size() - 1);
        }
    }
}