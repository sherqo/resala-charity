package com.resala.payment;

import java.time.YearMonth;
import java.util.UUID;

public final class CardPaymentMethod implements PaymentMethod {
  public String code() { return "CARD"; }
  public String displayName() { return "Card"; }
  public double feeRate() { return 0.0275; }

  @Override
  public boolean requiresToken() { return true; }

  @Override
  public void validate(PaymentCommand cmd) {
    if (cmd == null || cmd.token() == null || !cmd.token().startsWith("tok_card_")) {
      throw new IllegalArgumentException("Enter valid card details before paying by card.");
    }
  }

  @Override
  public String tokenize(PaymentInstrument instrument) {
    if (!(instrument instanceof CardDetails card)) {
      throw new IllegalArgumentException("Card details are required for a card payment.");
    }
    requireText(card.holder(), "Enter the card holder name.");
    if (card.number().length() < 13 || card.number().length() > 19) {
      throw new IllegalArgumentException("Card number must have 13 to 19 digits.");
    }
    if (!passesLuhn(card.number())) {
      throw new IllegalArgumentException("Enter a valid card number (try 4242 4242 4242 4242).");
    }
    if (parseExpiry(card.expiry()).isBefore(YearMonth.now())) {
      throw new IllegalArgumentException("This card has already expired.");
    }
    if (!card.cvv().matches("\\d{3,4}")) {
      throw new IllegalArgumentException("CVV must be 3 or 4 digits.");
    }
    return "tok_card_" + card.last4() + "_" + UUID.randomUUID();
  }

  private static void requireText(String value, String message) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
  }

  private static YearMonth parseExpiry(String expiry) {
    String digits = expiry == null ? "" : expiry.replaceAll("[^0-9]", "");
    if (digits.length() != 4 && digits.length() != 6) {
      throw new IllegalArgumentException("Enter the expiry date as MM/YY.");
    }
    int month = Integer.parseInt(digits.substring(0, 2));
    int year = Integer.parseInt(digits.substring(2));
    if (month < 1 || month > 12) {
      throw new IllegalArgumentException("Expiry month must be between 01 and 12.");
    }
    if (digits.length() == 4) {
      year += 2000;
    }
    return YearMonth.of(year, month);
  }

  private static boolean passesLuhn(String digits) {
    int sum = 0;
    boolean doubleDigit = false;
    for (int i = digits.length() - 1; i >= 0; i--) {
      int digit = digits.charAt(i) - '0';
      if (doubleDigit) {
        digit *= 2;
        if (digit > 9) digit -= 9;
      }
      sum += digit;
      doubleDigit = !doubleDigit;
    }
    return sum % 10 == 0;
  }
}
