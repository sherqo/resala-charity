package com.resala.payment;

/**
 * Dummy wallet details captured by the UI for demonstration purposes.
 * Validation and tokenization are owned by {@link WalletPaymentMethod}.
 */
public record WalletDetails(String phone) implements PaymentInstrument {

  public WalletDetails {
    phone = phone == null ? "" : phone.trim();
  }

  public String digits() {
    return phone.replaceAll("[^0-9]", "");
  }

  public String last4() {
    String digits = digits();
    return digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;
  }
}
