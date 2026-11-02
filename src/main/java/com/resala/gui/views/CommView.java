package com.resala.gui.views;

import com.resala.gui.AppShell;
import com.resala.gui.BaseView;
import com.resala.gui.components.DataTable;
import com.resala.gui.components.FormDialog;
import com.resala.gui.components.Toolbar;
import com.resala.gui.components.Toast;
import com.resala.notification.EmailStrategy;
import com.resala.notification.Notifier;
import com.resala.notification.NotificationStrategy;
import com.resala.notification.SmsStrategy;
import com.resala.notification.SocialStrategy;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Messages view - demonstrates Strategy pattern for notification channels.
 * Extends BaseView but doesn't use the table/toolbar from BaseView.
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
        setPadding(new Insets(24));
        setSpacing(16);

        // Remove the dummy table from the view
        getChildren().remove(1); // Remove the table

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
        // Channel selection card
        VBox channelCard = new VBox(12);
        channelCard.getStyleClass().add("card");
        channelCard.setPadding(new Insets(16));

        Label channelTitle = new Label("Notification Channel");
        channelTitle.getStyleClass().add("card-title");

        channelGroup = new ToggleGroup();
        HBox radioBox = new HBox(20);
        radioBox.setAlignment(Pos.CENTER_LEFT);

        RadioButton emailRadio = new RadioButton("📧 Email");
        emailRadio.setToggleGroup(channelGroup);
        emailRadio.setUserData("email");
        emailRadio.setSelected(true);

        RadioButton smsRadio = new RadioButton("📱 SMS");
        smsRadio.setToggleGroup(channelGroup);
        smsRadio.setUserData("sms");

        RadioButton socialRadio = new RadioButton("🌐 Social");
        socialRadio.setToggleGroup(channelGroup);
        socialRadio.setUserData("social");

        radioBox.getChildren().addAll(emailRadio, smsRadio, socialRadio);

        // Update strategy when selection changes
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

        channelCard.getChildren().addAll(channelTitle, radioBox);

        // Message form card
        VBox formCard = new VBox(12);
        formCard.getStyleClass().add("card");
        formCard.setPadding(new Insets(16));

        Label formTitle = new Label("Compose Message");
        formTitle.getStyleClass().add("card-title");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        Label recipientLabel = new Label("Recipient:");
        recipientLabel.getStyleClass().add("form-label");
        TextField recipientField = new TextField();
        recipientField.getStyleClass().add("input");
        recipientField.setPromptText("email@example.com or +201001234567 or @username");

        Label subjectLabel = new Label("Subject:");
        subjectLabel.getStyleClass().add("form-label");
        TextField subjectField = new TextField();
        subjectField.getStyleClass().add("input");
        subjectField.setPromptText("Subject / Title");

        Label bodyLabel = new Label("Message:");
        bodyLabel.getStyleClass().add("form-label");
        TextArea bodyArea = new TextArea();
        bodyArea.getStyleClass().add("input");
        bodyArea.setPrefRowCount(4);
        bodyArea.setWrapText(true);
        bodyArea.setPromptText("Message body...");

        grid.add(recipientLabel, 0, 0);
        grid.add(recipientField, 1, 0);
        grid.add(subjectLabel, 0, 1);
        grid.add(subjectField, 1, 1);
        grid.add(bodyLabel, 0, 2);
        grid.add(bodyArea, 1, 2);
        javafx.scene.layout.GridPane.setHgrow(recipientField, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.GridPane.setHgrow(subjectField, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.GridPane.setHgrow(bodyArea, javafx.scene.layout.Priority.ALWAYS);

        Button sendBtn = Toolbar.primary("Send Message", e -> {
            String recipient = recipientField.getText().trim();
            String subject = subjectField.getText().trim();
            String body = bodyArea.getText().trim();

            if (recipient.isEmpty() || body.isEmpty()) {
                appShell.getToast().show(appShell.getStage(),
                    "Recipient and message are required.", Toast.Type.WARNING);
                return;
            }

            notifier.send(recipient, subject, body);
            logList.getItems().add(0, "[" + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                + "] " + channelGroup.getSelectedToggle().getUserData().toString().toUpperCase()
                + " to " + recipient + ": " + subject);
            while (logList.getItems().size() > 100) logList.getItems().remove(logList.getItems().size() - 1);

            appShell.getToast().show(appShell.getStage(),
                "Message sent via " + channelGroup.getSelectedToggle().getUserData(), Toast.Type.SUCCESS);
        });

        formCard.getChildren().addAll(formTitle, grid, sendBtn);

        // Log card
        VBox logCard = new VBox(8);
        logCard.getStyleClass().add("card");
        logCard.setPadding(new Insets(16));
        VBox.setVgrow(logCard, javafx.scene.layout.Priority.ALWAYS);

        Label logTitle = new Label("Message Log");
        logTitle.getStyleClass().add("card-title");

        logList = new ListView<>();
        logList.getStyleClass().add("feed-list-view");
        VBox.setVgrow(logList, javafx.scene.layout.Priority.ALWAYS);

        logCard.getChildren().addAll(logTitle, logList);

        getChildren().addAll(channelCard, formCard, logCard);
    }

    @Override
    public void refresh() {
        // Stateless view - no refresh needed
    }
}