package com.resala.notification;

import com.resala.core.AuditLogManager;
import java.util.Objects;

/**
 * Holds the active NotificationStrategy and delegates send calls to it.
 * The strategy can be swapped at runtime without touching callers.
 *
 * Usage:
 *   Notifier notifier = new Notifier(new EmailStrategy());
 *   notifier.send("user@example.com", "Thank you", "We received your donation.");
 *   notifier.setStrategy(new SmsStrategy());
 *   notifier.send("+201001234567", "Thank you", "We received your donation.");
 */
public class Notifier {

  private NotificationStrategy strategy;

  public Notifier(NotificationStrategy strategy) {
    this.strategy = Objects.requireNonNull(strategy, "Strategy must not be null");
  }

  /** Swap the active channel at runtime. */
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

  /** Delegates to whichever strategy is currently active. */
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
