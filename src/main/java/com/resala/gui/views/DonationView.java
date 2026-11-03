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
import com.resala.gui.components.ModalOverlay;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.model.Donor;
import com.resala.model.DonationType;
import com.resala.donation.DonationStatus;
import com.resala.payment.CardDetails;
import com.resala.payment.CardPaymentMethod;
import com.resala.payment.PaymentInstrument;
import com.resala.payment.PaymentMethod;
import com.resala.payment.PaymentOutcome;
import com.resala.payment.PaymentStatus;
import com.resala.payment.WalletDetails;
import com.resala.payment.WalletPaymentMethod;
import com.resala.notification.DonationObserver;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DonationView extends BaseView implements DonationObserver {

    private final AppShell appShell;
    private final DonationController donationController;
    private final PaymentController paymentController;

    private final DataTable<Donation> donationsTable;
    private DonationFilter currentFilter;

    @SuppressWarnings("unchecked")
    public DonationView(AppShell appShell,
                        DonationController donationController,
                        PaymentController paymentController) {
        super(createToolbar(), createDonationsTable());

        this.appShell = appShell;
        this.donationController = donationController;
        this.paymentController = paymentController;
        this.donationsTable = (DataTable<Donation>) this.table;

        // Secondary actions in toolbar
        Button receiptBtn = Toolbar.ghost("View Receipt", e -> showReceiptDialog());
        Button editBtn = Toolbar.ghost("Edit", e -> editSelected());
        Button deleteBtn = Toolbar.ghost("Delete", e -> deleteSelected());
        Button filterBtn = Toolbar.ghost("Filter", e -> showFilterDialog());
        toolbar.getChildren().addAll(receiptBtn, editBtn, deleteBtn, filterBtn);

        this.primaryActionButton = Toolbar.primary("Add Donation", e -> showAddDonationDialog());
    }

    @Override
    public void onDonationRecorded(Donation donation, Donor donor) {
        Platform.runLater(this::refresh);
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
            new FormDialog.Field("amountMinor", "Amount (EGP)", FormDialog.Type.NUMBER, true),
            new FormDialog.Field("referenceNumber", "Check / external reference (optional)", FormDialog.Type.TEXT, false),
            new FormDialog.Field("itemDescription", "Item description (in-kind only)", FormDialog.Type.TEXT, false),
            new FormDialog.Field("quantity", "Quantity (in-kind only)", FormDialog.Type.NUMBER, false),
            new FormDialog.Field("estimatedValueMinor", "Estimated value EGP (in-kind only)", FormDialog.Type.NUMBER, false),
            new FormDialog.Field("notes", "Notes", FormDialog.Type.TEXTAREA, false)
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Add Donation", fields);
        result.ifPresent(map -> {
            try {
                long donorId = Long.parseLong(((String) map.get("donorId")).split(":")[0]);
                DonationType type = DonationType.valueOf((String) map.get("type"));
                long amountMinor = parseMinorUnits(map.get("amountMinor"));
                String notes = blankToNull((String) map.get("notes"));

                if (type == DonationType.ONLINE) {
                    payOnline(donorId, amountMinor, notes);
                    return;
                }

                String referenceNumber = blankToNull((String) map.get("referenceNumber"));
                String itemDescription = blankToNull((String) map.get("itemDescription"));
                Integer quantity = null;
                String qtyText = (String) map.get("quantity");
                if (qtyText != null && !qtyText.isBlank()) {
                    quantity = Integer.parseInt(qtyText.trim());
                }
                Long estimatedValueMinor = null;
                String estText = (String) map.get("estimatedValueMinor");
                if (estText != null && !estText.isBlank()) {
                    estimatedValueMinor = parseMinorUnits(estText);
                }
                DonationRequest request;
                if (type == DonationType.IN_KIND) {
                    if (itemDescription == null || itemDescription.isBlank()) {
                        throw new IllegalArgumentException("Item description is required for in-kind donations.");
                    }
                    if (quantity == null || quantity <= 0) {
                        throw new IllegalArgumentException("Quantity must be positive for in-kind donations.");
                    }
                    request = new DonationRequest(donorId, type, 0L, itemDescription, quantity,
                        estimatedValueMinor, null, LocalDate.now(), notes);
                } else {
                    // Check: referenceNumber is the check number
                    request = new DonationRequest(donorId, type, amountMinor, null, null, null,
                        referenceNumber, LocalDate.now(), notes);
                }
                donationController.recordDonation(request);
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Donation recorded successfully!", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    /** Lets the user pick a gateway method, collect dummy details, and charge the payment. */
    private void payOnline(long donorId, long amountMinor, String notes) {
        PaymentMethod method = choosePaymentMethod();
        if (method == null) {
            appShell.getToast().show(appShell.getStage(), "Payment cancelled.", Toast.Type.INFO);
            return;
        }

        PaymentInstrument instrument = (method instanceof CardPaymentMethod)
            ? cardDetails()
            : (method instanceof WalletPaymentMethod)
                ? walletDetails()
                : null;
        boolean needsDetails = method.requiresToken();
        if (needsDetails && instrument == null) {
            appShell.getToast().show(appShell.getStage(), "Payment cancelled.", Toast.Type.INFO);
            return;
        }
        // The method strategy validates the raw details and issues the one-time token.
        String token = method.tokenize(instrument);

        String reference = UUID.randomUUID().toString();
        PaymentOutcome outcome = paymentController.payDonation(
            donorId, amountMinor, method, token, reference, notes);

        while (outcome.status() == PaymentStatus.PENDING && outcome.canRetry()) {
            boolean retry = appShell.getConfirmDialog().ask(appShell.getStage(),
                "Payment is pending:\n" + outcome.message() + "\n\nRetry with the same reference?");
            if (!retry) {
                appShell.getToast().show(appShell.getStage(),
                    "Payment pending. Reference: " + reference, Toast.Type.WARNING);
                return;
            }
            outcome = paymentController.payDonation(donorId, amountMinor, method, token, reference, notes);
        }

        if (!outcome.isPaid()) {
            appShell.getToast().show(appShell.getStage(),
                "Payment failed: " + outcome.message(), Toast.Type.ERROR);
            return;
        }

        refresh();
        long feeMinor = paymentController.getPayment(outcome.paymentId()).getFeeMinor();
        String feeText = feeMinor > 0 ? " (fee " + String.format("%.2f", feeMinor / 100.0) + " EGP)" : "";
        appShell.getToast().show(appShell.getStage(),
            "Payment approved. Donation #" + outcome.referenceId() + " recorded." + feeText, Toast.Type.SUCCESS);
    }

    private PaymentMethod choosePaymentMethod() {
        List<PaymentMethod> methods = paymentController.availableMethods();
        if (methods.isEmpty()) {
            throw new IllegalStateException("No payment methods are available.");
        }
        var fields = List.of(new FormDialog.Field("method", "Payment method", FormDialog.Type.COMBO, true,
            methods.stream().map(PaymentMethod::displayName).toArray()));
        Optional<Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Payment Method", fields);
        if (result.isEmpty()) return null;
        String chosen = (String) result.get().get("method");
        return methods.stream().filter(m -> m.displayName().equals(chosen)).findFirst().orElse(null);
    }

    /** Dummy card form; validation and tokenization happen in {@link com.resala.payment.CardPaymentMethod}. */
    private CardDetails cardDetails() {
        var fields = List.of(
            new FormDialog.Field("holder", "Card holder", FormDialog.Type.TEXT, true),
            new FormDialog.Field("number", "Card number", FormDialog.Type.TEXT, true, "4242 4242 4242 4242"),
            new FormDialog.Field("expiry", "Expiry (MM/YY)", FormDialog.Type.TEXT, true, "12/30"),
            new FormDialog.Field("cvv", "CVV", FormDialog.Type.TEXT, true, "123")
        );
        Optional<Map<String, Object>> result = FormDialog.show(
            appShell.getStage(), "Card Details (dummy data - nothing is charged)", fields);
        if (result.isEmpty()) return null;
        Map<String, Object> values = result.get();
        return new CardDetails(
            (String) values.get("holder"),
            (String) values.get("number"),
            (String) values.get("expiry"),
            (String) values.get("cvv"));
    }

    /** Dummy wallet form; validation and tokenization happen in {@link com.resala.payment.WalletPaymentMethod}. */
    private WalletDetails walletDetails() {
        var fields = List.of(
            new FormDialog.Field("phone", "Wallet phone", FormDialog.Type.TEXT, true, "01012345678")
        );
        Optional<Map<String, Object>> result = FormDialog.show(
            appShell.getStage(), "Wallet Details (dummy data)", fields);
        if (result.isEmpty()) return null;
        return new WalletDetails((String) result.get().get("phone"));
    }

    private static long parseMinorUnits(Object value) {
        String text = value == null ? "" : value.toString().trim();
        if (text.isEmpty()) throw new IllegalArgumentException("Enter the amount.");
        try {
            return new java.math.BigDecimal(text).movePointRight(2).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Enter a valid amount, for example 250.00.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
        if (appShell != null && appShell.getModalOverlay() != null) {
            ModalOverlay.show(appShell, new ModalOverlay.ModalCard<Void>() {
                private java.util.function.Consumer<Void> closeCallback;

                @Override
                public Node getNode() {
                    VBox card = new VBox();
                    card.getStyleClass().add("modal-card");
                    card.setMaxWidth(560);
                    card.setMinWidth(480);
                    card.setMaxHeight(560);

                    // Header
                    HBox header = new HBox();
                    header.getStyleClass().add("modal-header");
                    header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label titleLabel = new Label("Donation Receipt");
                    titleLabel.getStyleClass().add("modal-title");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button closeBtn = new Button("✕");
                    closeBtn.getStyleClass().add("modal-close-btn");
                    closeBtn.setOnAction(e -> {
                        if (closeCallback != null) closeCallback.accept(null);
                    });

                    header.getChildren().addAll(titleLabel, spacer, closeBtn);

                    // Body
                    VBox body = new VBox(10);
                    body.getStyleClass().add("modal-body");

                    TextArea area = new TextArea(text);
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.getStyleClass().add("preview-area");
                    VBox.setVgrow(area, Priority.ALWAYS);
                    body.getChildren().add(area);
                    VBox.setVgrow(body, Priority.ALWAYS);

                    // Footer
                    HBox footer = new HBox(8);
                    footer.getStyleClass().add("modal-footer");
                    footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                    Region footerSpacer = new Region();
                    HBox.setHgrow(footerSpacer, Priority.ALWAYS);

                    Button doneBtn = new Button("Close");
                    doneBtn.getStyleClass().addAll("btn", "btn-ghost");
                    doneBtn.setOnAction(e -> {
                        if (closeCallback != null) closeCallback.accept(null);
                    });

                    footer.getChildren().addAll(footerSpacer, doneBtn);
                    card.getChildren().addAll(header, body, footer);
                    return card;
                }

                @Override
                public void setCloseHandler(java.util.function.Consumer<Void> closeCallback) {
                    this.closeCallback = closeCallback;
                }
            });
            return;
        }

        // Fallback
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(appShell.getStage());
        dialog.setTitle("Donation Receipt");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setContent(new TextArea(text));
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
            try {
                donationController.deleteDonation(selected.getId());
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(), "Donation #" + selected.getId() + " deleted.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        }
    }

    private void editSelected() {
        Donation selected = donationsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a donation first.", Toast.Type.WARNING);
            return;
        }
        if (selected.getStatus() != DonationStatus.PENDING
            && selected.getStatus() != DonationStatus.COMPLETED) {
            appShell.getToast().show(appShell.getStage(),
                "Cannot edit donation in status " + selected.getStatus(), Toast.Type.ERROR);
            return;
        }

        var fields = List.of(
            new FormDialog.Field("amountMinor", "Amount (EGP)", FormDialog.Type.NUMBER, true,
                String.format("%.2f", selected.getAmountMinor() / 100.0)),
            new FormDialog.Field("referenceNumber", "Reference number", FormDialog.Type.TEXT, false,
                selected.getReferenceNumber() != null ? selected.getReferenceNumber() : ""),
            new FormDialog.Field("donationDate", "Donation date YYYY-MM-DD", FormDialog.Type.TEXT, true,
                selected.getDonationDate().toString()),
            new FormDialog.Field("notes", "Notes", FormDialog.Type.TEXTAREA, false,
                selected.getNotes() != null ? selected.getNotes() : "")
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Edit Donation", fields);
        result.ifPresent(map -> {
            try {
                long amountMinor = parseMinorUnits(map.get("amountMinor"));
                String referenceNumber = blankToNull((String) map.get("referenceNumber"));
                LocalDate donationDate = LocalDate.parse(((String) map.get("donationDate")).trim());
                String notes = blankToNull((String) map.get("notes"));

                DonationRequest request = new DonationRequest(
                    selected.getDonorId(), selected.getType(), amountMinor,
                    selected.getType() == DonationType.IN_KIND
                        ? (selected.getItemDescription() != null ? selected.getItemDescription() : "")
                        : null,
                    selected.getType() == DonationType.IN_KIND ? selected.getQuantity() : null,
                    selected.getType() == DonationType.IN_KIND ? selected.getEstimatedValueMinor() : null,
                    referenceNumber, donationDate, notes
                );

                Donation updated = donationController.updateDonation(request, selected.getId());
                appShell.refreshAll();
                appShell.getToast().show(appShell.getStage(),
                    "Donation #" + updated.getId() + " updated.", Toast.Type.SUCCESS);
            } catch (Exception ex) {
                appShell.getToast().show(appShell.getStage(), "Error: " + ex.getMessage(), Toast.Type.ERROR);
            }
        });
    }

    @Override
    public void refresh() {
        DonationFilter filter = currentFilter != null ? currentFilter : new DonationFilter(null, null, null, null);
        List<Donation> donations = donationController.findDonations(filter);
        donationsTable.getTableView().setItems(FXCollections.observableArrayList(donations));
    }

    private void showFilterDialog() {
        var fields = List.of(
            new FormDialog.Field("type", "Type", FormDialog.Type.COMBO, true,
                "All", "ONLINE", "CHECK", "IN_KIND")
        );

        Optional<java.util.Map<String, Object>> result = FormDialog.show(appShell.getStage(), "Filter Donations", fields);
        result.ifPresent(map -> {
            String typeText = (String) map.get("type");
            if ("All".equals(typeText)) {
                currentFilter = null;
            } else {
                currentFilter = new DonationFilter(null,
                    DonationType.valueOf(typeText), null, null);
            }
            refresh();
        });
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