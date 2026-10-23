package com.resala.payment;

public final class CashPaymentMethod implements PaymentMethod {
  public String code() { return "CASH"; }
  public String displayName() { return "Cash"; }
  public double feeRate() { return 1.0; }
  public void validate(PaymentCommand cmd) { /* cash needs no token */ }
}
 
