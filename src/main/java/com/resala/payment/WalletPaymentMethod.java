package com.resala.payment;

import java.util.UUID;

public final class WalletPaymentMethod implements PaymentMethod {
  public String code() { return "WALLET"; }
  public String displayName() { return "Wallet"; }
  public double feeRate() { return 0.015; }

  @Override
  public boolean requiresToken() { return true; }

  @Override
  public void validate(PaymentCommand cmd) {
    if (cmd == null || cmd.token() == null || !cmd.token().startsWith("tok_wallet_")) {
      throw new IllegalArgumentException("Enter a valid wallet number before paying by wallet.");
    }
  }

  @Override
  public String tokenize(PaymentInstrument instrument) {
    if (!(instrument instanceof WalletDetails wallet)) {
      throw new IllegalArgumentException("A wallet phone number is required for a wallet payment.");
    }
    String digits = wallet.digits();
    if (digits.length() < 10 || digits.length() > 15) {
      throw new IllegalArgumentException("Enter a valid wallet phone number (e.g. 01012345678).");
    }
    return "tok_wallet_" + wallet.last4() + "_" + UUID.randomUUID();
  }
}
