package com.resala.payment;

public final class CardPaymentMethod implements PaymentMethod {
  public String code() { return "CARD"; }
  public String displayName() { return "Card"; }
  public double feeRate() { return 0.0275; }
  public void validate(PaymentCommand cmd) { /* minimum: no card-data validation */ }
}


