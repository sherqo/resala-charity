package com.resala.event;

public class EventException extends RuntimeException {

  private final EventError error;

  public EventException(EventError error, String detail) {
    super(detail == null || detail.isBlank()
        ? error.message()
        : error.message() + " " + detail,
        null);
    this.error = error;
  }

  public EventException(EventError error, String detail, Throwable cause) {
    super(detail == null || detail.isBlank()
        ? error.message()
        : error.message() + " " + detail,
        cause);
    this.error = error;
  }

  public EventError error() { return error; }

  public String code()      { return error.code(); }


  public boolean retryable() { return error.retryable(); }
}
