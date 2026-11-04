package com.resala.gui.views;

import com.resala.core.AuditLogManager;
import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.ModalOverlay;
import com.resala.gui.components.Toast;
import com.resala.gui.components.Toolbar;
import com.resala.model.AuditLog;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

public class AuditLogView extends BaseView {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FULL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppShell appShell;
    private final FilteredList<AuditLog> filteredLogs;
    private final DataTable<AuditLog> logTable;
    private final ComboBox<String> categoryFilter;
    private final TextField searchField;
    private final Button primaryActionButton;

    @SuppressWarnings("unchecked")
    public AuditLogView(AppShell appShell) {
        this(appShell, new FilteredList<>(AuditLogManager.getInstance().getLogs(), log -> true));
    }

    @SuppressWarnings("unchecked")
    private AuditLogView(AppShell appShell, FilteredList<AuditLog> filteredLogs) {
        super(createToolbar(), createLogTable(filteredLogs));

        this.appShell = appShell;
        this.filteredLogs = filteredLogs;
        this.logTable = (DataTable<AuditLog>) this.table;

        this.categoryFilter = new ComboBox<>();
        categoryFilter.getItems().addAll(
            "All Categories",
            "Observer Triggers",
            "Outgoing Notifications",
            "Payment Operations"
        );
        categoryFilter.getSelectionModel().selectFirst();
        categoryFilter.setPrefWidth(190);

        this.searchField = new TextField();
        searchField.setPromptText("Search logs & payload...");
        searchField.setPrefWidth(220);

        Button detailsBtn = Toolbar.ghost("View Details", e -> showSelectedDetails());

        Button clearBtn = Toolbar.danger("Clear Logs", e -> {
            AuditLogManager.getInstance().clear();
            if (appShell != null) {
                appShell.getToast().show(appShell.getStage(), "Audit log cleared", Toast.Type.INFO);
            }
        });

        Label categoryLabel = new Label("Category:");
        categoryLabel.getStyleClass().add("filter-label");

        toolbar.getChildren().addAll(categoryLabel, categoryFilter, searchField, detailsBtn, clearBtn);

        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        logTable.getTableView().setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                showSelectedDetails();
            }
        });

        this.primaryActionButton = Toolbar.primary("Refresh", e -> refresh());
    }

    private static HBox createToolbar() {
        return Toolbar.of();
    }

    private static DataTable<AuditLog> createLogTable(FilteredList<AuditLog> filteredLogs) {
        return DataTable.<AuditLog>builder()
            .column("Time", 85, log -> log.getTimestamp() != null ? TIME_FORMATTER.format(log.getTimestamp()) : "")
            .column("Pattern", 120, AuditLog::getPattern)
            .column("Module", 120, AuditLog::getModule)
            .column("Category", 160, AuditLog::getCategory)
            .column("Recipient", 180, AuditLog::getRecipient)
            .column("Summary", 320, AuditLog::getSummary)
            .items(filteredLogs)
            .build();
    }

    private void applyFilter() {
        String selectedCategory = categoryFilter.getValue();
        String query = searchField.getText();

        filteredLogs.setPredicate(log -> {
            if (log == null) return false;

            if (selectedCategory != null && !selectedCategory.equals("All Categories")) {
                if (!selectedCategory.equalsIgnoreCase(log.getCategory())) {
                    return false;
                }
            }

            if (query != null && !query.isBlank()) {
                String q = query.toLowerCase().trim();
                boolean matches = (log.getPattern() != null && log.getPattern().toLowerCase().contains(q))
                        || (log.getModule() != null && log.getModule().toLowerCase().contains(q))
                        || (log.getCategory() != null && log.getCategory().toLowerCase().contains(q))
                        || (log.getRecipient() != null && log.getRecipient().toLowerCase().contains(q))
                        || (log.getSummary() != null && log.getSummary().toLowerCase().contains(q))
                        || (log.getDetails() != null && log.getDetails().toLowerCase().contains(q));
                if (!matches) {
                    return false;
                }
            }

            return true;
        });
    }

    private void showSelectedDetails() {
        AuditLog selected = logTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (appShell != null) {
                appShell.getToast().show(appShell.getStage(),
                    "Please select a log entry to view details", Toast.Type.WARNING);
            }
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("RESALA AUDIT & PATTERN LOG ENTRY\n");
        sb.append("========================================\n\n");
        sb.append(String.format("Log ID:       #%d\n", selected.getId()));
        sb.append(String.format("Timestamp:    %s\n", selected.getTimestamp() != null ? FULL_TIME_FORMATTER.format(selected.getTimestamp()) : "N/A"));
        sb.append(String.format("Pattern:      %s\n", selected.getPattern()));
        sb.append(String.format("Module:       %s\n", selected.getModule()));
        sb.append(String.format("Category:     %s\n", selected.getCategory()));
        sb.append(String.format("Recipient:    %s\n", selected.getRecipient()));
        sb.append("\nSummary:\n");
        sb.append(selected.getSummary()).append("\n\n");
        sb.append("----------------------------------------\n");
        sb.append("Payload / Details:\n");
        sb.append("----------------------------------------\n");
        sb.append(selected.getDetails() != null ? selected.getDetails() : "(No details provided)").append("\n");
        sb.append("\n========================================\n");

        String displayText = sb.toString();

        if (appShell != null && appShell.getModalOverlay() != null) {
            ModalOverlay.show(appShell, new ModalOverlay.ModalCard<Void>() {
                private java.util.function.Consumer<Void> closeCallback;

                @Override
                public Node getNode() {
                    VBox card = new VBox();
                    card.getStyleClass().add("modal-card");
                    card.setMaxWidth(620);
                    card.setMinWidth(480);
                    card.setMaxHeight(600);

                    HBox header = new HBox();
                    header.getStyleClass().add("modal-header");
                    header.setAlignment(Pos.CENTER_LEFT);

                    Label titleLabel = new Label("Audit Entry #" + selected.getId() + " (" + selected.getPattern() + ")");
                    titleLabel.getStyleClass().add("modal-title");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button closeBtn = new Button("✕");
                    closeBtn.getStyleClass().add("modal-close-btn");
                    closeBtn.setOnAction(e -> {
                        if (closeCallback != null) closeCallback.accept(null);
                    });

                    header.getChildren().addAll(titleLabel, spacer, closeBtn);

                    VBox body = new VBox(10);
                    body.getStyleClass().add("modal-body");

                    TextArea area = new TextArea(displayText);
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.getStyleClass().add("preview-area");
                    VBox.setVgrow(area, Priority.ALWAYS);
                    body.getChildren().add(area);
                    VBox.setVgrow(body, Priority.ALWAYS);

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
    public Button getPrimaryAction() {
        return primaryActionButton;
    }

    @Override
    public void refresh() {
        applyFilter();
    }
}
