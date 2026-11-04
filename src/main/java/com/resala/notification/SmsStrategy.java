package com.resala.notification;

public class SmsStrategy implements NotificationStrategy {

  @Override
  public void send(String recipient, String subject, String body) {
    // SMS messages do not have a separate subject.
    String message = subject.isBlank() ? body : subject + ": " + body;
    System.out.println("[SMS] To: " + recipient);
    System.out.println("      " + message);
  }
}
