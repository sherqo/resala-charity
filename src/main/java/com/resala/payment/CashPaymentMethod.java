package com.resala.payment;

public final class CashPaymentMethod implements PaymentMethod {
  public String code() { return "CASH"; }
  public String displayName() { return "Cash"; }
  public double feeRate() { return 0.0; }

  @Override
  public void validate(PaymentCommand cmd) {
    // Cash is collected at the counter, so no payment token is required.
  }

  @Override
  public String tokenize(PaymentInstrument instrument) {
    if (instrument != null) {
      throw new IllegalArgumentException("Cash payments do not need payment details.");
    }
    return null;
  }
}
