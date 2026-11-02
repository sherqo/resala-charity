package com.resala.payment;

/**
 * Payment-method strategy. Each implementation owns the rules for its own
 * instrument: what details it needs, how they are validated, and how a
 * one-time token is issued for the gateway.
 */
public interface PaymentMethod {
  String code();
  String displayName();
  double feeRate();

  /** Charge-time check: does this command carry a token this method accepts? */
  void validate(PaymentCommand cmd);

  /** Whether this method needs a one-time token before charging. */
  default boolean requiresToken() { return false; }

  /**
   * Capture-time check: validates the raw UI details for this method and
   * returns the one-time token handed to the gateway. Returns null for cash.
   */
  String tokenize(PaymentInstrument instrument);
}
