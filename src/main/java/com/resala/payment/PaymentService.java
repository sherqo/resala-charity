package com.resala.payment;

import com.resala.core.TransactionManager;

import java.util.List;
import java.util.Optional;

public class PaymentService {

  private final PaymentRepository payments;
  private final PaymentGateway gateway;
  private final TransactionManager tx;
  private final PaymentSettlement settlement; // optional, may be null

  public PaymentService(PaymentRepository payments, PaymentGateway gateway,
                        TransactionManager tx, PaymentSettlement settlement) {
    this.payments = payments;
    this.gateway = gateway;
    this.tx = tx;
    this.settlement = settlement;
  }

  public PaymentOutcome pay(PaymentRequest request) {
    validate(request);

    Optional<Payment> existing = payments.findByIdempotencyKey(request.idempotencyKey());
    if (existing.isPresent()) {
      return resumeOrReturn(existing.get(), request);
    }

    Payment payment = Payment.createPending(request.payerType(), request.payerId(), request.amountMinor(),
        request.currency(), request.paymentMethod(), request.idempotencyKey(), request.notes());
    payments.save(payment);

    GatewayResult result;
    try {
      result = gateway.charge(new PaymentCommand(
          payment.getIdempotencyKey(), payment.getAmountMinor(), payment.getCurrency(),
          payment.getPaymentMethod(), request.description()));
    } catch (RuntimeException e) {
      payment.markFailed(PaymentError.PROVIDER_UNAVAILABLE, "Unexpected gateway error");
      payments.save(payment);
      return PaymentOutcome.failed(payment);
    }

    if (!result.approved()) {
      payment.markFailed(result.failureReason(), result.message());
      payments.save(payment);
      return PaymentOutcome.failed(payment);
    }
    if (result.amountMinor() != payment.getAmountMinor()
        || !result.currency().equals(payment.getCurrency())) {
      payment.markFailed(PaymentError.AMOUNT_MISMATCH, result.message());
      payments.save(payment);
      return PaymentOutcome.failed(payment);
    }

    if (settlement != null) {
      try {
        tx.inTransaction(() -> {
          long recordId = settlement.settle(payment);
          payment.markPaid(settlement.referenceType(), recordId);
          payments.save(payment);
          return recordId;
        });
        return PaymentOutcome.paid(payment);
      } catch (RuntimeException e) {
        throw new PaymentException(PaymentError.PERSISTENCE_FAILURE, payment.getId(),
            "approved but not recorded", e);
      }
    }

    payment.markPaid("NONE", 0);
    payments.save(payment);
    return PaymentOutcome.paid(payment);
  }

  public List<PaymentMethod> availableMethods() {
    return gateway.getAvailableMethods();
  }

  public List<Payment> history(String payerType, long payerId) {
    return payments.findByPayer(payerType, payerId);
  }

  public Payment get(long paymentId) {
    return payments.findById(paymentId)
        .orElseThrow(() -> new PaymentException(PaymentError.INVALID_STATE, paymentId, "not found", null));
  }

  private PaymentOutcome resumeOrReturn(Payment existing, PaymentRequest request) {
    if (existing.getStatus() == PaymentStatus.PAID) {
      return PaymentOutcome.paid(existing);
    }
    if (existing.getStatus() == PaymentStatus.FAILED) {
      return PaymentOutcome.failed(existing);
    }
    // PENDING: re-charge with the same key; provider idempotency prevents a double charge.
    return pay(new PaymentRequest(existing.getIdempotencyKey(), request.payerType(), existing.getPayerId(),
        existing.getAmountMinor(), existing.getCurrency(), existing.getPaymentMethod(),
        request.description(), request.notes()));
  }

  private void validate(PaymentRequest request) {
    if (request == null) throw new IllegalArgumentException("request is required");
    if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
      throw new IllegalArgumentException("idempotencyKey is required");
    }
    if (request.payerType() == null || request.payerType().isBlank()) {
      throw new IllegalArgumentException("payerType is required");
    }
    if (request.payerId() <= 0) throw new IllegalArgumentException("payerId must be positive");
    if (request.amountMinor() <= 0) throw new PaymentException(PaymentError.INVALID_AMOUNT, null,
        "amount=" + request.amountMinor(), null);
    if (!"EGP".equals(request.currency())) throw new PaymentException(PaymentError.INVALID_CURRENCY, null,
        "currency=" + request.currency(), null);
    if (request.paymentMethod() == null) throw new IllegalArgumentException("paymentMethod is required");
  }
}