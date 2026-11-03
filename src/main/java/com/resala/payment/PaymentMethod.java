package com.resala.payment;

public interface PaymentMethod {
  String code();
  String displayName();
  double feeRate();

  /** Charge-time check: does this command carry a token this method accepts? */
  void validate(PaymentCommand cmd);

  /** Whether this method needs a one-time token before charging. */
  default boolean requiresToken() { return false; }

  String tokenize(PaymentInstrument instrument);
}
