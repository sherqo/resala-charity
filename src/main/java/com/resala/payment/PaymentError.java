package com.resala.payment;

public enum PaymentError {
  INVALID_AMOUNT("PAY-003", "Enter an amount greater than zero.", false),
  INVALID_CURRENCY("PAY-004", "Only EGP is supported.", false),
  IDEMPOTENCY_CONFLICT("PAY-005", "This payment key was already used with different details.", false),
  INVALID_STATE("PAY-006", "This payment can no longer change state.", false),
  AMOUNT_MISMATCH("PAY-007", "The provider returned a different amount.", false),
  PERSISTENCE_FAILURE("PAY-009", "Payment could not be recorded. Try again.", true),
  PROVIDER_DECLINED("PAY-101", "Payment was declined.", true),
  PROVIDER_TIMEOUT("PAY-102", "The payment provider timed out. You can retry.", true),
  PROVIDER_UNAVAILABLE("PAY-103", "The payment provider is unavailable. You can retry.", true);

  private final String code;
  private final String message;
  private final boolean retryable;

  PaymentError(String code, String message, boolean retryable) {
    this.code = code;
    this.message = message;
    this.retryable = retryable;
  }

  public String message() { return message; }
  public boolean retryable() { return retryable; }
}
