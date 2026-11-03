package com.resala.notification;

import com.resala.model.Event;

public interface EventObserver {
  void onEventUpdated(Event event, String message);
}
