package com.resala.notification;

public class SocialStrategy implements NotificationStrategy {

  @Override
  public void send(String recipient, String subject, String body) {
    System.out.println("[SOCIAL] @" + recipient);
    System.out.println("         " + subject + " — " + body);
  }
}
