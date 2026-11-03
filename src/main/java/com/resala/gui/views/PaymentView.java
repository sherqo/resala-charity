package com.resala.gui.views;

import com.resala.controller.PaymentController;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.ModalOverlay;
import com.resala.gui.components.Toast;
import com.resala.gui.components.Toolbar;
import com.resala.payment.Payment;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PaymentView extends BaseView {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final AppShell appShell;
    private final PaymentController paymentController;
    private final DataTable<Payment> paymentsTable;
    private final Button primaryActionButton;

    @SuppressWarnings("unchecked")
    public PaymentView(AppShell appShell, PaymentController paymentController) {
        super(createToolbar(), createPaymentsTable());

        this.appShell = appShell;
        this.paymentController = paymentController;
        this.paymentsTable = (DataTable<Payment>) this.table;

        Button detailsBtn = Toolbar.ghost("View Details", e -> showSelectedDetails());
        toolbar.getChildren().addAll(detailsBtn);

        this.primaryActionButton = Toolbar.primary("Refresh", e -> refresh());

        // Double-click row handler
        paymentsTable.getTableView().setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                showSelectedDetails();
            }
        });
    }

    @Override
    public Button getPrimaryAction() {
        return primaryActionButton;
    }

    private static HBox createToolbar() {
        return Toolbar.of();
    }

    private static DataTable<Payment> createPaymentsTable() {
        return DataTable.<Payment>builder()
            .column("ID", 60, (java.util.function.Function<Payment, String>) p -> String.valueOf(p.getId()))
            .column("Payer", 120, p -> p.getPayerType() + " #" + p.getPayerId())
            .column("Amount", 120, p -> String.format("%.2f %s", p.getAmountMinor() / 100.0, p.getCurrency()))
            .column("Fee", 90, p -> String.format("%.2f %s", p.getFeeMinor() / 100.0, p.getCurrency()))
            .column("Method", 100, Payment::getPaymentMethodCode)
            .column("Status", 90, p -> p.getStatus().name())
            .column("Reference Key", 200, Payment::getIdempotencyKey)
            .column("Created At", 140, p -> DATE_TIME_FORMATTER.format(p.getCreatedAt()))
            .column("Settled Ref", 120, p -> p.getReferenceType() != null ? p.getReferenceType() + " #" + p.getReferenceId() : "-")
            .items(FXCollections.observableArrayList())
            .build();
    }

    private void showSelectedDetails() {
        Payment selected = paymentsTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            appShell.getToast().show(appShell.getStage(), "Select a payment first.", Toast.Type.WARNING);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("        PAYMENT TRANSACTION RECORD      \n");
        sb.append("========================================\n\n");
        sb.append(String.format("Payment ID:       %d\n", selected.getId()));
        sb.append(String.format("Status:           %s\n", selected.getStatus()));
        sb.append(String.format("Payer:            %s #%d\n", selected.getPayerType(), selected.getPayerId()));
        sb.append(String.format("Amount:           %.2f %s\n", selected.getAmountMinor() / 100.0, selected.getCurrency()));
        sb.append(String.format("Gateway Fee:      %.2f %s\n", selected.getFeeMinor() / 100.0, selected.getCurrency()));
        sb.append(String.format("Net Amount:       %.2f %s\n", (selected.getAmountMinor() - selected.getFeeMinor()) / 100.0, selected.getCurrency()));
        sb.append(String.format("Payment Method:   %s\n", selected.getPaymentMethodCode()));
        sb.append(String.format("Idempotency Key:  %s\n", selected.getIdempotencyKey()));
        sb.append(String.format("Created At:       %s\n", DATE_TIME_FORMATTER.format(selected.getCreatedAt())));
        sb.append(String.format("Updated At:       %s\n", DATE_TIME_FORMATTER.format(selected.getUpdatedAt())));
        if (selected.getPaidAt() != null) {
            sb.append(String.format("Paid At:          %s\n", DATE_TIME_FORMATTER.format(selected.getPaidAt())));
        }
        if (selected.getReferenceType() != null) {
            sb.append(String.format("Settled Entity:   %s #%d\n", selected.getReferenceType(), selected.getReferenceId()));
        }
        if (selected.getFailureReason() != null) {
            sb.append(String.format("Failure Code:     %s\n", selected.getFailureReason()));
            sb.append(String.format("Failure Message:  %s\n", selected.getFailureMessage()));
        }
        if (selected.getNotes() != null && !selected.getNotes().isBlank()) {
            sb.append(String.format("Notes:            %s\n", selected.getNotes()));
        }
        sb.append("\n========================================\n");

        String receiptText = sb.toString();

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
                    header.setAlignment(Pos.CENTER_LEFT);

                    Label titleLabel = new Label("Payment Details #" + selected.getId());
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

                    TextArea area = new TextArea(receiptText);
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.getStyleClass().add("preview-area");
                    VBox.setVgrow(area, Priority.ALWAYS);
                    body.getChildren().add(area);
                    VBox.setVgrow(body, Priority.ALWAYS);

                    // Footer
                    HBox footer = new HBox(8);
                    footer.getStyleClass().add("modal-footer");
                    footer.setAlignment(Pos.CENTER_RIGHT);

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
        }
    }

    @Override
    public void refresh() {
        if (paymentController != null) {
            List<Payment> list = paymentController.listPayments();
            paymentsTable.getTableView().setItems(FXCollections.observableArrayList(list));
        } else {
            paymentsTable.getTableView().setItems(FXCollections.observableArrayList());
        }
    }
}
