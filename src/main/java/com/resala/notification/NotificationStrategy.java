package com.resala.notification;

public interface NotificationStrategy {
  void send(String recipient, String subject, String body);
}
