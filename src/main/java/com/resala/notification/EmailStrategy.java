package com.resala.notification;

/**
 * Concrete Strategy — email channel (mocked).
 * A real implementation would use JavaMail or an SMTP client here.
 */
public class EmailStrategy implements NotificationStrategy {

  @Override
  public void send(String recipient, String subject, String body) {
    System.out.println("[EMAIL] To: " + recipient);
    System.out.println("        Subject: " + subject);
    System.out.println("        " + body);
  }
}
