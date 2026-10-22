package com.resala.notification;

/**
 * Concrete Strategy — social media channel (mocked).
 * A real implementation would call a platform API (Facebook, X, etc.) here.
 */
public class SocialStrategy implements NotificationStrategy {

  @Override
  public void send(String recipient, String subject, String body) {
    System.out.println("[SOCIAL] @" + recipient);
    System.out.println("         " + subject + " — " + body);
  }
}
