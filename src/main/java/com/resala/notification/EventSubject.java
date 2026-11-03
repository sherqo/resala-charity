package com.resala.notification;

public interface EventSubject {
  void addObserver(EventObserver observer);
  void removeObserver(EventObserver observer);
}
