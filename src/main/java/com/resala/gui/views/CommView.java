package com.resala.gui.views;

import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.Toast;
import com.resala.gui.components.Toolbar;
import com.resala.notification.EmailStrategy;
import com.resala.notification.Notifier;
import com.resala.notification.SmsStrategy;
import com.resala.notification.SocialStrategy;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Messages view - demonstrates Strategy pattern for multi-channel notifications.
 * Notion-inspired layout: flat visual hierarchy, cohesive composer, no emojis.
 */
public class CommView extends BaseView {

    private final AppShell appShell;
    private final Notifier notifier;
    private ListView<String> logList;
    private ToggleGroup channelGroup;

    public CommView(AppShell appShell) {
        super(Toolbar.of(), createDummyTable());
        this.appShell = appShell;
        this.notifier = new Notifier(new EmailStrategy());

        getStyleClass().add("view");
        setPadding(new Insets(20, 24, 20, 24));
        setSpacing(20);

        // Remove the dummy table from the view
        getChildren().remove(1);

        buildLayout();
    }

    private static DataTable<String> createDummyTable() {
        return DataTable.<String>builder()
            .column("Dummy", 100, (java.util.function.Function<String, String>) s -> s)
            .items(FXCollections.observableArrayList())
            .build();
    }

    @Override
    public Button getPrimaryAction() {
        return null;
    }

    private void buildLayout() {
        // --- 1. Composer Section (Single unified card) ---
        VBox composerCard = new VBox(14);
        composerCard.getStyleClass().add("card");
        composerCard.setPadding(new Insets(18, 20, 18, 20));

        Label composeTitle = new Label("Compose Notification");
        composeTitle.getStyleClass().add("card-title");

        // Channel Selection (Restrained radio group, no emojis)
        HBox channelRow = new HBox(16);
        channelRow.setAlignment(Pos.CENTER_LEFT);

        Label channelLabel = new Label("Channel:");
        channelLabel.getStyleClass().add("form-label");
        channelLabel.setMinWidth(80);

        channelGroup = new ToggleGroup();

        RadioButton emailRadio = new RadioButton("Email");
        emailRadio.setToggleGroup(channelGroup);
        emailRadio.setUserData("email");
        emailRadio.setSelected(true);

        RadioButton smsRadio = new RadioButton("SMS");
        smsRadio.setToggleGroup(channelGroup);
        smsRadio.setUserData("sms");

        RadioButton socialRadio = new RadioButton("Social");
        socialRadio.setToggleGroup(channelGroup);
        socialRadio.setUserData("social");

        HBox radioOptions = new HBox(14, emailRadio, smsRadio, socialRadio);
        radioOptions.setAlignment(Pos.CENTER_LEFT);
        channelRow.getChildren().addAll(channelLabel, radioOptions);

        channelGroup.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (nw != null) {
                String channel = (String) nw.getUserData();
                switch (channel) {
                    case "email" -> notifier.setStrategy(new EmailStrategy());
                    case "sms" -> notifier.setStrategy(new SmsStrategy());
                    case "social" -> notifier.setStrategy(new SocialStrategy());
                }
            }
        });

        // Form Fields Grid
        GridPane formGrid = new GridPane();
        formGrid.setHgap(16);
        formGrid.setVgap(12);

        Label recipientLabel = new Label("Recipient:");
        recipientLabel.getStyleClass().add("form-label");
        recipientLabel.setMinWidth(80);

        TextField recipientField = new TextField();
        recipientField.getStyleClass().add("input");
        recipientField.setPromptText("email@example.com or +201001234567 or @handle");

        Label subjectLabel = new Label("Subject:");
        subjectLabel.getStyleClass().add("form-label");

        TextField subjectField = new TextField();
        subjectField.getStyleClass().add("input");
        subjectField.setPromptText("Subject line");

        Label bodyLabel = new Label("Message:");
        bodyLabel.getStyleClass().add("form-label");

        TextArea bodyArea = new TextArea();
        bodyArea.getStyleClass().add("text-area");
        bodyArea.setPrefRowCount(4);
        bodyArea.setWrapText(true);
        bodyArea.setPromptText("Write your notification content...");

        formGrid.add(recipientLabel, 0, 0);
        formGrid.add(recipientField, 1, 0);
        formGrid.add(subjectLabel, 0, 1);
        formGrid.add(subjectField, 1, 1);
        formGrid.add(bodyLabel, 0, 2);
        formGrid.add(bodyArea, 1, 2);

        GridPane.setHgrow(recipientField, Priority.ALWAYS);
        GridPane.setHgrow(subjectField, Priority.ALWAYS);
        GridPane.setHgrow(bodyArea, Priority.ALWAYS);

        // Action row
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        Button sendBtn = Toolbar.primary("Send Notification", e -> {
            String recipient = recipientField.getText().trim();
            String subject = subjectField.getText().trim();
            String body = bodyArea.getText().trim();

            if (recipient.isEmpty() || body.isEmpty()) {
                appShell.getToast().show(appShell.getStage(),
                    "Recipient and message are required.", Toast.Type.WARNING);
                return;
            }

            notifier.send(recipient, subject, body);

            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String channel = channelGroup.getSelectedToggle().getUserData().toString().toUpperCase();
            String logEntry = String.format("[%s] %s to %s — %s", time, channel, recipient, subject.isEmpty() ? "(no subject)" : subject);

            logList.getItems().add(0, logEntry);
            while (logList.getItems().size() > 100) {
                logList.getItems().remove(logList.getItems().size() - 1);
            }

            recipientField.clear();
            subjectField.clear();
            bodyArea.clear();

            appShell.getToast().show(appShell.getStage(),
                "Notification sent via " + channel, Toast.Type.SUCCESS);
        });

        actionRow.getChildren().add(sendBtn);

        composerCard.getChildren().addAll(composeTitle, channelRow, formGrid, actionRow);

        // --- 2. Activity / Delivery Log Section (Flat layout) ---
        VBox logSection = new VBox(8);
        VBox.setVgrow(logSection, Priority.ALWAYS);

        Label logTitle = new Label("Delivery Log");
        logTitle.getStyleClass().add("card-title");

        logList = new ListView<>();
        logList.getStyleClass().add("feed-list-view");
        logList.setPlaceholder(new Label("No messages sent yet in this session."));
        VBox.setVgrow(logList, Priority.ALWAYS);

        logSection.getChildren().addAll(logTitle, logList);

        getChildren().addAll(composerCard, logSection);
    }

    @Override
    public void refresh() {
        // Stateless view - no remote refresh needed
    }
}
