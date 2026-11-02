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
    try {
      payments.save(payment);
    } catch (RuntimeException e) {
      // Another request may have inserted the same key between our lookup and save.
      Optional<Payment> raced = payments.findByIdempotencyKey(request.idempotencyKey());
      if (raced.isPresent()) return resumeOrReturn(raced.get(), request);
      throw e;
    }

    return charge(payment, request.description());
  }

  private PaymentOutcome charge(Payment payment, String description) {
    GatewayResult result;
    try {
      result = gateway.charge(new PaymentCommand(
          payment.getIdempotencyKey(), payment.getAmountMinor(), payment.getCurrency(),
          payment.getPaymentMethod(), description));
    } catch (RuntimeException e) {
      // The provider may have accepted the charge. Keep the payment pending for a same-key retry.
      return PaymentOutcome.pending(payment, PaymentError.PROVIDER_UNAVAILABLE, "Unexpected gateway error");
    }

    if (!result.approved()) {
      PaymentError reason = result.failureReason();
      if (isAmbiguous(reason)) {
        return PaymentOutcome.pending(payment, reason, result.message());
      }
      payment.markFailed(reason, result.message());
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
      long recordId;
      try {
        recordId = tx.inTransaction(() -> {
          // Mark paid first so the settlement can verify the completed payment.
          payment.markPaid("NONE", 0);
          payments.save(payment);
          long settledId = settlement.settle(payment);
          payment.attachReference(settlement.referenceType(), settledId);
          payments.save(payment);
          return settledId;
        });
      } catch (RuntimeException e) {
        throw new PaymentException(PaymentError.PERSISTENCE_FAILURE, payment.getId(),
            "approved but not recorded", e);
      }
      try {
        settlement.afterCommit(payment, recordId);
      } catch (RuntimeException ignored) {
        // The payment is already committed; a notification failure must not turn it into a failure.
      }
      return PaymentOutcome.paid(payment);
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
    if (!matches(existing, request)) {
      throw new PaymentException(PaymentError.IDEMPOTENCY_CONFLICT, existing.getId(),
          "idempotency key was already used with different payment details", null);
    }
    if (existing.getStatus() == PaymentStatus.PAID) {
      return PaymentOutcome.paid(existing);
    }
    if (existing.getStatus() == PaymentStatus.FAILED) {
      return PaymentOutcome.failed(existing);
    }
    // PENDING: re-charge with the same key; provider idempotency prevents a double charge.
    return charge(existing, request.description());
  }

  private boolean matches(Payment payment, PaymentRequest request) {
    return payment.getPayerType().equals(request.payerType())
        && payment.getPayerId() == request.payerId()
        && payment.getAmountMinor() == request.amountMinor()
        && payment.getCurrency().equals(request.currency())
        && payment.getPaymentMethodCode().equals(request.paymentMethod().code());
  }

  /** A timeout or provider outage may still have charged the card, so the same key must be retried. */
  private boolean isAmbiguous(PaymentError reason) {
    return reason == PaymentError.PROVIDER_TIMEOUT
        || reason == PaymentError.PROVIDER_UNAVAILABLE;
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