package com.resala.payment;

public interface PaymentSettlement {
  String referenceType(); // e.g. "DONATION", "TICKET", "INVOICE"
  long settle(Payment payment);
}
