package com.resala.payment;

public class PaymentException extends RuntimeException {
  private final PaymentError error;
  private final Long paymentId;

  public PaymentException(PaymentError error, Long paymentId, String detail, Throwable cause) {
    super(detail == null ? error.message() : detail, cause);
    this.error = error;
    this.paymentId = paymentId;
  }

  public PaymentError error() { return error; }
  public Long paymentId() { return paymentId; }
  public String userMessage() { return error.message(); }
}
