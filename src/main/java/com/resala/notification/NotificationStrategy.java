package com.resala.notification;

/**
 * Strategy interface for sending notifications.
 * Each concrete strategy represents a different communication channel.
 */
public interface NotificationStrategy {
  /**
   * Sends a notification through this channel.
   *
   * @param recipient the address/number/handle to deliver to (email, phone, username)
   * @param subject   short subject line or title
   * @param body      full message body
   */
  void send(String recipient, String subject, String body);
}
