package com.resala.payment;

public final class CashPaymentMethod implements PaymentMethod {
  public String code() { return "CASH"; }
  public String displayName() { return "Cash"; }
  public double feeRate() { return 0.0; }

  @Override
  public void validate(PaymentCommand cmd) {
    // Cash payments do not need a token.
  }

  @Override
  public String tokenize(PaymentInstrument instrument) {
    if (instrument != null) {
      throw new IllegalArgumentException("Cash payments do not need payment details.");
    }
    return null;
  }
}
