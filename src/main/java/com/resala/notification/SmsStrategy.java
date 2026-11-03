package com.resala.notification;

public class SmsStrategy implements NotificationStrategy {

  @Override
  public void send(String recipient, String subject, String body) {
    // SMS has no subject — combine into a single short message
    String message = subject.isBlank() ? body : subject + ": " + body;
    System.out.println("[SMS] To: " + recipient);
    System.out.println("      " + message);
  }
}
