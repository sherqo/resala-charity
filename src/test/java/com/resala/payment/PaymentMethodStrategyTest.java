package com.resala.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaymentMethodStrategyTest {

  private final CardPaymentMethod card = new CardPaymentMethod();
  private final WalletPaymentMethod wallet = new WalletPaymentMethod();
  private final CashPaymentMethod cash = new CashPaymentMethod();

  @Test
  void cardStrategyAcceptsDummyDetailsAndIssuesACardToken() {
    String token = card.tokenize(new CardDetails("Test Donor", "4242 4242 4242 4242", "12/30", "123"));

    assertTrue(token.startsWith("tok_card_4242_"));
  }

  @Test
  void cardStrategyRejectsANumberThatFailsTheLuhnCheck() {
    CardDetails bad = new CardDetails("Test Donor", "4242 4242 4242 4241", "12/30", "123");

    assertThrows(IllegalArgumentException.class, () -> card.tokenize(bad));
  }

  @Test
  void cardStrategyRejectsAnExpiredCard() {
    CardDetails expired = new CardDetails("Test Donor", "4242 4242 4242 4242", "01/20", "123");

    assertThrows(IllegalArgumentException.class, () -> card.tokenize(expired));
  }

  @Test
  void cardStrategyRejectsAnInvalidCvv() {
    CardDetails badCvv = new CardDetails("Test Donor", "4242 4242 4242 4242", "12/30", "12");

    assertThrows(IllegalArgumentException.class, () -> card.tokenize(badCvv));
  }

  @Test
  void cardStrategyRejectsABlankHolder() {
    CardDetails blank = new CardDetails(" ", "4242 4242 4242 4242", "12/30", "123");

    assertThrows(IllegalArgumentException.class, () -> card.tokenize(blank));
  }

  @Test
  void cardStrategyRejectsAWalletInstrument() {
    WalletDetails details = new WalletDetails("01012345678");

    assertThrows(IllegalArgumentException.class, () -> card.tokenize(details));
  }

  @Test
  void walletStrategyAcceptsAPhoneNumberAndIssuesAWalletToken() {
    String token = wallet.tokenize(new WalletDetails("010 1234 5678"));

    assertTrue(token.startsWith("tok_wallet_5678_"));
  }

  @Test
  void walletStrategyRejectsAShortPhoneNumber() {
    WalletDetails bad = new WalletDetails("12345");

    assertThrows(IllegalArgumentException.class, () -> wallet.tokenize(bad));
  }

  @Test
  void cashStrategyNeedsNoInstrument() {
    assertNull(cash.tokenize(null));
    assertThrows(IllegalArgumentException.class,
        () -> cash.tokenize(new CardDetails("Test Donor", "4242 4242 4242 4242", "12/30", "123")));
  }

  @Test
  void chargeTimeValidationIsAlsoOwnedByTheStrategy() {
    PaymentCommand withoutToken = new PaymentCommand(
        "key-1", 5_000L, "EGP", card, null, "test");
    PaymentCommand withToken = new PaymentCommand(
        "key-1", 5_000L, "EGP", card, card.tokenize(
            new CardDetails("Test Donor", "4242 4242 4242 4242", "12/30", "123")), "test");

    assertThrows(IllegalArgumentException.class, () -> card.validate(withoutToken));
    card.validate(withToken);
    cash.validate(withoutToken);
  }
}
