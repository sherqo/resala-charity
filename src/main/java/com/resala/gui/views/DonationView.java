package com.resala.gui.views;

import com.resala.controller.DonationController;
import com.resala.controller.PaymentController;
import com.resala.donation.Donation;
import com.resala.donation.DonationFilter;
import com.resala.donation.DonationRequest;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.Donor;
import com.resala.model.DonationType;
import com.resala.payment.PaymentService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Donations view - manage donations and receipts.
 */
public class DonationView extends BaseView {

    private final AppShell appShell;
    private final DonationController donationController;
    private final PaymentController paymentController;
    private final PaymentService paymentService;

    private DataTable<Donation> donationsTable;

    public DonationView(AppShell appShell,
                        DonationController donationController,
                        PaymentController paymentController,
                        PaymentService paymentService) {
        super(createToolbar(), createDonationsTable());

        this.appShell = appShell;
        this.donationController = donationController;
        this.paymentController = paymentController;
        this.paymentService = paymentService;

        this.donationsTable = (DataTable<Donation>) getTableView().getProperties().get("dataTable");
        if (this.donationsTable == null) {
            this.donationsTable = createDonationsTable();
            getTableView().getProperties().put("dataTable", this.donationsTable);
        }

        this.primaryActionButton = Toolbar.primary("+ Add Donation", e -> showAddDonationDialog());
    }

    private Button primaryActionButton;

    @Override
    public Button getPrimaryAction() {
        return primaryActionButton;
    }

    private static HBox createToolbar() {
        return Toolbar.of();
    }

    private static DataTable<Donation> createDonationsTable() {
        return DataTable.<Donation>builder()
            .column("ID", 60, (java.util.function.Function<Donation, String>) d -> String.valueOf(d.getId()))
            .column("Donor", 180, (java.util.function.Function<Donation, String>) d -> "Donor #" + d.getDonorId())
            .column("Amount", 100, (java.util.function.Function<Donation, String>) d -> formatAmount(d))
            .column("Method", 100, (java.util.function.Function<Donation, String>) d -> d.getType().name())
            .column("Reference", 150, (java.util.function.Function<Donation, String>) d -> d.getReferenceNumber() != null ? d.getReferenceNumber() : "")
            .column("Date", 120, (java.util.function.Function<Donation, String>) d -> d.getDonationDate().toString())
            .items(FXCollections.observableArrayList())
            .onRowDoubleClick(d -> {})
            .build();
    }

    private void showAddDonationDialog() {
        List<Donor> donors = donationController.findDonors("");
        if (donors.isEmpty()) {
            appShell.getToast().show(appShell.getStage(), "No donors available. Add a donor first.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("donorId", "Donor", FormDialog.Type.COMBO, true,
                donors.stream().map(d -> d.getId() + ": " + d.getName()).toArray()),
            new FormDialog.Field("type", "Type", FormDialog.Type.COMBO, true,
                DonationType.ONLINE.name(), DonationType.CHECK.name(), DonationType.IN_KIND.name()),
            new FormDialog.Field("amountMinor", "Amount (minor units)", FormDialog.Type.NUMBER, true),
            new FormDialog.Field("referenceNumber", "Payment Reference", FormDialog.Type.TEXT, false),
            new FormDialog.Field("notes", "Notes", FormDialog.Type.TEXTAREA, false)
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Add Donation", fields);
        result.ifPresent(map -> {
            try {
                long donorId = Long.parseLong(((String) map.get("donorId")).split(":")[0]);
                DonationType type = DonationType.valueOf((String) map.get("type"));
                long amountMinor = Long.parseLong(map.get("amountMinor").toString());
                String referenceNumber = (String) map.get("referenceNumber");
                String notes = (String) map.get("notes");

                DonationRequest request;
                if (type == DonationType.IN_KIND) {
                    // In-kind: itemDescription, quantity, estimatedValueMinor
                    request = new DonationRequest(donorId, type, amountMinor, "In-kind donation", 1, amountMinor, null, java.time.LocalDate.now(), notes);
                } else {
                    // Online/Check: referenceNumber
                    request = new DonationRequest(donorId, type, amountMinor, null, null, null, referenceNumber, java.time.LocalDate.now(), notes);
                }
                Donation donation = donationController.recordDonation(request);

                refresh();
                appShell.getToast().show(appShell.getStage(), "Donation recorded successfully!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showReceiptDialog() {
        Donation selected = donationsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a donation first.", Toast.Type.WARNING);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("tax", "Include Tax Receipt", FormDialog.Type.CHECKBOX, false, "Tax Receipt"),
            new FormDialog.Field("thankYou", "Include Thank You Note", FormDialog.Type.CHECKBOX, false, "Thank You Note")
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Receipt Options", fields);
        result.ifPresent(map -> {
            try {
                String receiptText = donationController.receipt(selected.getId());
                showReceiptPreview(receiptText);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    private void showReceiptPreview(String text) {
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(20);
        area.setPrefColumnCount(60);

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(appShell.getStage());
        dialog.setTitle("Receipt Preview");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        dialog.showAndWait();
    }

    private void deleteSelected() {
        Donation selected = donationsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a donation first.", Toast.Type.WARNING);
            return;
        }

        if (appShell.getConfirmDialog().ask(appShell.getStage(),
                "Delete donation #" + selected.getId() + "?")) {
            appShell.getToast().show(appShell.getStage(), "Delete not implemented yet.", Toast.Type.INFO);
        }
    }

    @Override
    public void refresh() {
        List<Donation> donations = donationController.findDonations(new DonationFilter(null, null, null, null));
        donationsTable.getTableView().setItems(FXCollections.observableArrayList(donations));
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