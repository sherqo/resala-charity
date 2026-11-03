package com.resala.notification;

public class EmailStrategy implements NotificationStrategy {

  @Override
  public void send(String recipient, String subject, String body) {
    System.out.println("[EMAIL] To: " + recipient);
    System.out.println("        Subject: " + subject);
    System.out.println("        " + body);
  }
}
