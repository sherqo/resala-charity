package com.resala.notification;

import com.resala.core.AuditLogManager;
import java.util.Objects;

public class Notifier {

  private NotificationStrategy strategy;

  public Notifier(NotificationStrategy strategy) {
    this.strategy = Objects.requireNonNull(strategy, "Strategy must not be null");
  }

  public void setStrategy(NotificationStrategy strategy) {
    this.strategy = Objects.requireNonNull(strategy, "Strategy must not be null");
    String channelName = strategy.getClass().getSimpleName().replace("Strategy", "");
    AuditLogManager.getInstance().log(
        "Strategy",
        "Notifications",
        "Outgoing Notifications",
        "System",
        "Swapped notification channel to " + channelName.toUpperCase(),
        "Runtime Strategy Pattern: active NotificationStrategy is now " + strategy.getClass().getSimpleName()
    );
  }

  public NotificationStrategy getStrategy() {
    return strategy;
  }

  public void send(String recipient, String subject, String body) {
    strategy.send(recipient, subject, body);

    String channelName = strategy.getClass().getSimpleName().replace("Strategy", "");
    AuditLogManager.getInstance().log(
        "Strategy (" + channelName + ")",
        "Notifications",
        "Outgoing Notifications",
        recipient,
        subject != null && !subject.isBlank() ? subject : "Notification dispatched",
        "Channel: " + channelName.toUpperCase() + "\nRecipient: " + recipient + "\nSubject: " + subject + "\n\nBody:\n" + body
    );
  }
}
