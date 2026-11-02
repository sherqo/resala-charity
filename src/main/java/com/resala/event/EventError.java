package com.resala.event;

/**
 * Catalogue of every error that can originate from the event module.
 * Each entry carries a stable error code, a user-friendly message, and a
 * flag indicating whether the caller may safely retry the operation.
 */
public enum EventError {

  // --- Input / validation ---
  TITLE_REQUIRED        ("EVT-001", "Event title is required.",                                    false),
  DATE_REQUIRED         ("EVT-002", "Event date is required.",                                     false),
  INVALID_EVENT_TYPE    ("EVT-003", "A valid event type must be selected.",                        false),
  INVALID_CAPACITY      ("EVT-004", "Capacity must be a positive number.",                         false),
  INVALID_DATE_FORMAT   ("EVT-005", "Date format is invalid. Use YYYY-MM-DD.",                     false),
  ATTENDEE_NAME_REQUIRED("EVT-006", "Attendee name is required.",                                  false),

  // --- Not-found ---
  EVENT_NOT_FOUND       ("EVT-010", "Event not found.",                                            false),
  REGISTRATION_NOT_FOUND("EVT-011", "Registration not found.",                                     false),

  // --- Lifecycle / business rule ---
  EVENT_ALREADY_OPEN    ("EVT-020", "Event is already OPEN.",                                      false),
  EVENT_NOT_OPEN        ("EVT-021", "Only OPEN events can be closed.",                             false),
  DELETE_NON_DRAFT      ("EVT-022", "Only DRAFT events with no active history can be deleted.",    false),
  CAPACITY_BELOW_REGISTERED("EVT-023", "New capacity cannot be less than existing registrations.", false),
  EVENT_FULL            ("EVT-024", "Event is at full capacity.",                                   false),
  REGISTRATION_CLOSED   ("EVT-025", "Registrations are only accepted for OPEN events.",            false),

  // --- Persistence ---
  PERSISTENCE_FAILURE   ("EVT-090", "A database error occurred. Please try again.",                true);

  private final String code;
  private final String message;
  private final boolean retryable;

  EventError(String code, String message, boolean retryable) {
    this.code      = code;
    this.message   = message;
    this.retryable = retryable;
  }

  /** Stable short code suitable for logging and support tickets. */
  public String code()      { return code; }

  /** User-friendly message describing the problem. */
  public String message()   { return message; }

  /** Whether the same operation can be retried without changing the request. */
  public boolean retryable() { return retryable; }
}
