package com.resala.payment;

public interface PaymentSettlement {
  String referenceType(); // e.g. "DONATION", "TICKET", "INVOICE"
  long settle(Payment payment);

  /** Runs after the settlement transaction commits, so side effects like notifications are safe. */
  default void afterCommit(Payment payment, long recordId) {}
}
