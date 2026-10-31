package com.resala.payment;

public final class WalletPaymentMethod implements PaymentMethod {
  public String code() { return "WALLET"; }
  public String displayName() { return "Wallet"; }
  public double feeRate() { return 0.015; }
  public void validate(PaymentCommand cmd) { /* minimum: no wallet-id validation */ }
}
