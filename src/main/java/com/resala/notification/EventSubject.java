package com.resala.notification;

/**
 * Subject interface for the event module.
 * Allows observers to subscribe and unsubscribe from event-related updates.
 */
public interface EventSubject {
  void addObserver(EventObserver observer);
  void removeObserver(EventObserver observer);
}
