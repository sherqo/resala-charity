package com.resala.payment;

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
  public static PaymentOutcome pending(Payment p, PaymentError error, String message) {
    return new PaymentOutcome(p.getId(), PaymentStatus.PENDING, null, message, error);
  }
  public static PaymentOutcome rejected(PaymentException e) {
    return new PaymentOutcome(e.paymentId() == null ? 0 : e.paymentId(), null, null,
        e.userMessage(), e.error());
  }
}