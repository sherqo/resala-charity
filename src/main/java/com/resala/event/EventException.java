package com.resala.event;

/**
 * Typed runtime exception for all errors originating in the event module.
 *
 * <p>Each instance wraps an {@link EventError} that carries a stable error code,
 * a user-friendly message, and a retry flag.  The optional {@code detail} field
 * lets callers append context (e.g. the conflicting values) while still
 * keeping the enum message as the primary user-visible text.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 *   throw new EventException(EventError.EVENT_NOT_FOUND, "id=" + id);
 * }</pre>
 */
public class EventException extends RuntimeException {

  private final EventError error;

  /**
   * Creates an EventException with an optional extra detail string appended to
   * the enum's standard message.
   *
   * @param error  the specific error kind
   * @param detail additional context (may be {@code null})
   */
  public EventException(EventError error, String detail) {
    super(detail == null || detail.isBlank()
        ? error.message()
        : error.message() + " " + detail,
        null);
    this.error = error;
  }

  /**
   * Creates an EventException that wraps a lower-level cause (e.g. a
   * {@code SQLException} from the persistence layer).
   *
   * @param error  the specific error kind
   * @param detail additional context (may be {@code null})
   * @param cause  the originating exception
   */
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
