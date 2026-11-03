package com.resala.payment;

import com.resala.core.AuditLogManager;
import com.resala.core.TransactionManager;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class PaymentService {

  private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());

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
    try {
      validate(request);
    } catch (PaymentException e) {
      return PaymentOutcome.rejected(e);
    } catch (IllegalArgumentException e) {
      return PaymentOutcome.rejected(new PaymentException(PaymentError.INVALID_STATE, null, e.getMessage(), e));
    }

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
      return PaymentOutcome.rejected(new PaymentException(PaymentError.PERSISTENCE_FAILURE, null,
          "Payment could not be recorded. Try again.", e));
    }

    return charge(payment, request.paymentToken(), request.description());
  }

  private PaymentOutcome charge(Payment payment, String token, String description) {
    GatewayResult result;
    try {
      result = gateway.charge(new PaymentCommand(
          payment.getIdempotencyKey(), payment.getAmountMinor(), payment.getCurrency(),
          payment.getPaymentMethod(), token, description));
    } catch (RuntimeException e) {
      // The provider may have accepted the charge. Keep the payment pending for a same-key retry.
      LOGGER.warning("Unexpected gateway error for payment " + payment.getId() + ": " + e);
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
        LOGGER.warning("Settlement failed for payment " + payment.getId() + ": " + e);
        return PaymentOutcome.pending(payment, PaymentError.PERSISTENCE_FAILURE, "approved but not recorded");
      }
      try {
        settlement.afterCommit(payment, recordId);
      } catch (RuntimeException e) {
        // The payment is already committed; a notification failure must not turn it into a failure.
        LOGGER.warning("afterCommit failed for payment " + payment.getId() + ": " + e);
      }
      AuditLogManager.getInstance().log(
          "Strategy & Command",
          "Payments",
          "Payment Operations",
          payment.getPayerType() + " #" + payment.getPayerId(),
          "Payment " + payment.getPaymentMethod().displayName() + " — " + (payment.getAmountMinor() / 100.0) + " " + payment.getCurrency(),
          "Payment Strategy: " + payment.getPaymentMethod().displayName() + "\n"
          + "Command: PaymentCommand\n"
          + "Settled with: " + settlement.referenceType() + " #" + recordId + "\n"
          + "Amount: " + (payment.getAmountMinor() / 100.0) + " " + payment.getCurrency() + "\n"
          + "Fee: " + (payment.getFeeMinor() / 100.0) + " " + payment.getCurrency() + "\n"
          + "Idempotency Key: " + payment.getIdempotencyKey() + "\n"
          + "Status: " + payment.getStatus()
      );
      return PaymentOutcome.paid(payment);
    }

    payment.markPaid("NONE", 0);
    payments.save(payment);
    AuditLogManager.getInstance().log(
        "Strategy & Command",
        "Payments",
        "Payment Operations",
        payment.getPayerType() + " #" + payment.getPayerId(),
        "Payment " + payment.getPaymentMethod().displayName() + " — " + (payment.getAmountMinor() / 100.0) + " " + payment.getCurrency(),
        "Payment Strategy: " + payment.getPaymentMethod().displayName() + "\n"
        + "Command: PaymentCommand\n"
        + "Amount: " + (payment.getAmountMinor() / 100.0) + " " + payment.getCurrency() + "\n"
        + "Fee: " + (payment.getFeeMinor() / 100.0) + " " + payment.getCurrency() + "\n"
        + "Idempotency Key: " + payment.getIdempotencyKey() + "\n"
        + "Status: " + payment.getStatus()
    );
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

  public List<Payment> listPayments() {
    return payments.findAll();
  }

  private PaymentOutcome resumeOrReturn(Payment existing, PaymentRequest request) {
    if (!matches(existing, request)) {
      return PaymentOutcome.rejected(new PaymentException(PaymentError.IDEMPOTENCY_CONFLICT, existing.getId(),
          "idempotency key was already used with different payment details", null));
    }
    if (existing.getStatus() == PaymentStatus.PAID) {
      return PaymentOutcome.paid(existing);
    }
    if (existing.getStatus() == PaymentStatus.FAILED) {
      return PaymentOutcome.failed(existing);
    }
    // PENDING: re-charge with the same key; provider idempotency prevents a double charge.
    return charge(existing, request.paymentToken(), request.description());
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
    request.paymentMethod().validate(new PaymentCommand(
        request.idempotencyKey(), request.amountMinor(), request.currency(),
        request.paymentMethod(), request.paymentToken(), request.description()));
  }
}
