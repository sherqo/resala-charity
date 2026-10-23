package com.resala.payment;

public record PaymentRequest(
    String idempotencyKey,
    String payerType,
    long payerId,
    long amountMinor,
    String currency,
    PaymentMethod paymentMethod,
    String description,
    String notes
) {}

public record PaymentCommand(
    String idempotencyKey,
    long amountMinor,
    String currency,
    PaymentMethod paymentMethod,
    String description
) {}

public record GatewayResult(
    boolean approved,
    long amountMinor,
    String currency,
    String message,
    PaymentError failureReason
) {
  public GatewayResult {
    if (approved && failureReason != null) throw new IllegalArgumentException("approved result cannot carry a failure reason");
    if (!approved && failureReason == null) throw new IllegalArgumentException("failed result must carry a failure reason");
  }
  public static GatewayResult approved(long amount, String currency, String message) {
    return new GatewayResult(true, amount, currency, message, null);
  }
  public static GatewayResult failed(PaymentError reason, String message, long amount, String currency) {
    return new GatewayResult(false, amount, currency, message, reason);
  }
}

public record PaymentOutcome(
    long paymentId,
    PaymentStatus status,
    Long referenceId,
    String message,
    PaymentError error
) {
  public boolean isPaid() { return status == PaymentStatus.PAID; }
  public boolean canRetry() { return error != null && error.retryable(); }
  public static PaymentOutcome paid(Payment p) {
    return new PaymentOutcome(p.getId(), PaymentStatus.PAID, p.getReferenceId(), "Payment approved", null);
  }
  public static PaymentOutcome failed(Payment p) {
    return new PaymentOutcome(p.getId(), PaymentStatus.FAILED, null,
        p.getFailureMessage(), p.getFailureReason());
  }
  public static PaymentOutcome rejected(PaymentException e) {
    return new PaymentOutcome(e.paymentId() == null ? 0 : e.paymentId(), null, null,
        e.userMessage(), e.error());
  }
}
```
