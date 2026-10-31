package com.resala.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.resala.core.TransactionManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

  private final FakePaymentRepository payments = new FakePaymentRepository();
  private final StubGateway gateway = new StubGateway();
  private final PaymentService service = new PaymentService(payments, gateway, new TransactionManager(null), null);

  @Test
  void retryableProviderFailureLeavesPaymentPending() {
    gateway.result = GatewayResult.failed(PaymentError.PROVIDER_TIMEOUT, "timeout", 5_000L, "EGP");

    PaymentOutcome outcome = service.pay(request("key-1", 5_000L));

    assertEquals(PaymentStatus.PENDING, outcome.status());
    assertTrue(outcome.canRetry());
    assertEquals(PaymentStatus.PENDING, payments.findByKey("key-1").orElseThrow().getStatus());
  }

  @Test
  void definitiveDeclineMarksPaymentFailed() {
    gateway.result = GatewayResult.failed(PaymentError.PROVIDER_DECLINED, "declined", 5_000L, "EGP");

    PaymentOutcome outcome = service.pay(request("key-1", 5_000L));

    assertEquals(PaymentStatus.FAILED, outcome.status());
    assertEquals(PaymentStatus.FAILED, payments.findByKey("key-1").orElseThrow().getStatus());
  }

  @Test
  void reusingAKeyWithDifferentDetailsIsRejected() {
    gateway.result = GatewayResult.approved(5_000L, "EGP", "ok");
    service.pay(request("key-1", 5_000L));

    PaymentException error = assertThrows(PaymentException.class,
        () -> service.pay(request("key-1", 6_000L)));

    assertEquals(PaymentError.IDEMPOTENCY_CONFLICT, error.error());
  }

  @Test
  void pendingPaymentIsRetriedDirectlyInsteadOfRecursing() {
    Payment pending = Payment.createPending(
        "DONOR", 7L, 5_000L, "EGP", new CashPaymentMethod(), "key-1", null);
    pending.setId(1L);
    payments.save(pending);
    gateway.result = GatewayResult.approved(5_000L, "EGP", "ok");

    PaymentOutcome outcome = service.pay(request("key-1", 5_000L));

    assertEquals(PaymentStatus.PAID, outcome.status());
    assertEquals(1, gateway.calls);
  }

  @Test
  void settlementSeesPaidPaymentAndReferenceIsAttached() throws Exception {
    gateway.result = GatewayResult.approved(5_000L, "EGP", "ok");
    AtomicBoolean sawPaid = new AtomicBoolean();
    AtomicBoolean afterCommit = new AtomicBoolean();
    PaymentSettlement settlement = new PaymentSettlement() {
      @Override public String referenceType() { return "DONATION"; }
      @Override public long settle(Payment payment) {
        sawPaid.set(payment.isPaid());
        return 42L;
      }
      @Override public void afterCommit(Payment payment, long recordId) {
        afterCommit.set(recordId == 42L);
      }
    };

    try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
      PaymentService settling = new PaymentService(
          payments, gateway, new TransactionManager(conn), settlement);

      PaymentOutcome outcome = settling.pay(request("key-1", 5_000L));

      assertTrue(sawPaid.get());
      assertTrue(afterCommit.get());
      assertEquals(42L, outcome.referenceId());
      assertEquals("DONATION", payments.findByKey("key-1").orElseThrow().getReferenceType());
    }
  }

  private PaymentRequest request(String key, long amountMinor) {
    return new PaymentRequest(key, "DONOR", 7L, amountMinor, "EGP",
        new CashPaymentMethod(), "test", null);
  }

  private static final class StubGateway implements PaymentGateway {
    private GatewayResult result;
    private int calls;

    @Override public List<PaymentMethod> getAvailableMethods() {
      return List.of(new CashPaymentMethod());
    }

    @Override public GatewayResult charge(PaymentCommand command) {
      calls++;
      return result;
    }
  }

  private static final class FakePaymentRepository implements PaymentRepository {
    private final Map<String, Payment> byKey = new HashMap<>();
    private long sequence;

    @Override public Payment save(Payment payment) {
      if (payment.getId() == 0) payment.setId(++sequence);
      byKey.put(payment.getIdempotencyKey(), payment);
      return payment;
    }

    @Override public Optional<Payment> findById(long id) {
      return byKey.values().stream().filter(p -> p.getId() == id).findFirst();
    }

    @Override public Optional<Payment> findByIdempotencyKey(String key) {
      return findByKey(key);
    }

    Optional<Payment> findByKey(String key) {
      return Optional.ofNullable(byKey.get(key));
    }

    @Override public List<Payment> findByPayer(String payerType, long payerId) {
      return byKey.values().stream()
          .filter(p -> p.getPayerType().equals(payerType) && p.getPayerId() == payerId)
          .toList();
    }

    @Override public List<Payment> findByStatus(PaymentStatus status) {
      return byKey.values().stream().filter(p -> p.getStatus() == status).toList();
    }
  }
}
