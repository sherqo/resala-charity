package com.resala.payment;

public enum PaymentError{
  DONOR_NOT_FOUND("PAY-001", "Donor not found.", false),
  DONOR_INACTIVE("PAY-002", "This donor is archived and cannot pay.", false),
  INVALID_AMOUNT("PAY-003", "Enter an amount greater than zero.", false),
  INVALID_CURRENCY("PAY-004", "Only EGP is supported.", false),
  INVALID_PAYMENT_TOKEN("PAY-005", "Choose a valid test payment token.", false),
  IDEMPOTENCY_CONFLICT("PAY-006", "This payment key was already used with different details.", false),
  INVALID_STATE("PAY-007", "This payment can no longer change state.", false),
  AMOUNT_MISMATCH("PAY-008", "The provider returned a different amount.", false),
  PAYMENT_EXPIRED("PAY-009", "Checkout expired. Start a new payment.", true),
  PERSISTENCE_FAILURE("PAY-010", "Payment could not be recorded. Try again.", true),
  PROVIDER_DECLINED("PAY-101", "Payment was declined. Try another token.", true),
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

  public String code() { return code; }
  public String message() { return message; }
  public boolean retryable() { return retryable; }
}
