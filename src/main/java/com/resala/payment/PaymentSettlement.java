package com.resala.payment;

public interface PaymentSettlement {
  String referenceType();
  long settle(Payment payment);

  default void afterCommit(Payment payment, long recordId) {}
}
