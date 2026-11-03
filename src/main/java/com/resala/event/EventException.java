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

  /** @return the {@link EventError} that describes this failure. */
  public EventError error() { return error; }

  /** @return the stable error code (e.g. {@code "EVT-010"}). */
  public String code()      { return error.code(); }

  /** @return the primary user-facing message from the {@link EventError}. */
  public String userMessage() { return error.message(); }

  /** @return {@code true} if the caller may safely retry the same operation. */
  public boolean retryable() { return error.retryable(); }
}
