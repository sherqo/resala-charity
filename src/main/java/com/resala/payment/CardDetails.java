package com.resala.payment;

public record CardDetails(String holder, String number, String expiry, String cvv)
    implements PaymentInstrument {

  public CardDetails {
    holder = holder == null ? "" : holder.trim();
    number = number == null ? "" : number.replaceAll("[^0-9]", "");
    expiry = expiry == null ? "" : expiry.trim();
    cvv = cvv == null ? "" : cvv.trim();
  }

  public String last4() {
    return number.length() >= 4 ? number.substring(number.length() - 4) : number;
  }
}
