package com.resala.notification;

import com.resala.model.Event;

/**
 * Observer interface to receive notifications when an event changes or is updated.
 */
public interface EventObserver {
  void onEventUpdated(Event event, String message);
}
