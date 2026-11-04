package com.resala.payment;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// This simulates payment results and does not move real money.
public class FakePaymentProvider {

  private final List<PaymentMethod> availableMethods =
      java.util.List.of(new CashPaymentMethod(), new CardPaymentMethod(), new WalletPaymentMethod());
  private final Map<String, ProviderTxn> byKey = new ConcurrentHashMap<>();
  private final AtomicLong sequence = new AtomicLong(1000);
  private final long latencyMillis;

  public FakePaymentProvider() { this(200); }
  public FakePaymentProvider(long latencyMillis) { this.latencyMillis = latencyMillis; }

  public List<PaymentMethod> availableMethods() { return availableMethods; }

  public ProviderTxn makePayment(String idempotencyKey, long cents, PaymentMethod method, String currency,
                                 String token) throws ProviderException {
    if (latencyMillis > 0) {
      try { Thread.sleep(latencyMillis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    if (method.requiresToken() && (token == null || token.isBlank())) {
      return new ProviderTxn("FAKE-REJECT", "05", "Missing payment token", cents, currency);
    }

    ProviderTxn existing = byKey.get(idempotencyKey);
    if (existing != null) return existing;

    int tail = (int) (Math.abs(cents) % 10);
    String code;
    String message;
    if (tail == 0) { code = "51"; message = "Insufficient funds"; }
    else if (tail == 5) { throw new ProviderException(ProviderError.TIMEOUT); }
    else if (tail == 7) { throw new ProviderException(ProviderError.NETWORK); }
    else { code = "00"; message = "Approved"; }

    ProviderTxn txn = new ProviderTxn("FAKE-" + method.code() + "-" + sequence.incrementAndGet(),
        code, message, cents, currency);
    byKey.put(idempotencyKey, txn);
    return txn;
  }

  public record ProviderTxn(String providerRef, String statusCode, String message,
                            long amountCents, String currency) {}

  public enum ProviderError { TIMEOUT, NETWORK }

  public static class ProviderException extends RuntimeException {
    public final ProviderError error;
    public ProviderException(ProviderError error) { super(error.name()); this.error = error; }
  }
}
