package com.resala.payment;

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
