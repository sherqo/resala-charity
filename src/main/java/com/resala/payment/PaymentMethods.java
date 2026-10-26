package com.resala.payment;

import java.util.List;
import java.util.Map;

public final class PaymentMethods {
  private static final Map<String, PaymentMethod> BY_CODE = Map.of(
      "CASH", new CashPaymentMethod(),
      "CARD", new CardPaymentMethod(),
      "WALLET", new WalletPaymentMethod());

  private PaymentMethods() {}

  public static PaymentMethod fromCode(String code) {
    PaymentMethod m = BY_CODE.get(code);
    if (m == null) throw new IllegalArgumentException("Unknown payment method: " + code);
    return m;
  }

  public static List<PaymentMethod> all() {
    return List.of(new CashPaymentMethod(), new CardPaymentMethod(), new WalletPaymentMethod());
  }
}
