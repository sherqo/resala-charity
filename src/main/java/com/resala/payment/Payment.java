package com.resala.payment;

import java.time.Instant;
import java.util.Objects;

public final class Payment {

  private long id;
  private final String payerType;
  private final long payerId;
  private final long amountMinor;
  private final String currency;
  private final String paymentMethodCode;
  private final String idempotencyKey;

  private PaymentStatus status;
  private String referenceType;
  private Long referenceId;
  private PaymentError failureReason;
  private String failureMessage;
  private String notes;

  private final Instant createdAt;
  private Instant updatedAt;
  private Instant paidAt;

  private Payment(String payerType, long payerId, long amountMinor, String currency,
                  String paymentMethodCode, String idempotencyKey, String notes, Instant now) {
    this.payerType = payerType;
    this.payerId = payerId;
    this.amountMinor = amountMinor;
    this.currency = currency;
    this.paymentMethodCode = paymentMethodCode;
    this.idempotencyKey = idempotencyKey;
    this.notes = notes;
    this.status = PaymentStatus.PENDING;
    this.createdAt = Objects.requireNonNull(now, "now");
    this.updatedAt = now;
  }

  public static Payment createPending(String payerType, long payerId, long amountMinor, String currency,
                                      PaymentMethod paymentMethod, String idempotencyKey, String notes) {
    if (payerType == null || payerType.isBlank()) throw new IllegalArgumentException("payerType is required");
    if (payerId <= 0) throw new IllegalArgumentException("payerId must be positive");
    if (amountMinor <= 0) throw new PaymentException(PaymentError.INVALID_AMOUNT, null,
        "amountMinor=" + amountMinor, null);
    if (!"EGP".equals(currency)) throw new PaymentException(PaymentError.INVALID_CURRENCY, null,
        "currency=" + currency, null);
    if (paymentMethod == null) throw new IllegalArgumentException("paymentMethod is required");
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("idempotencyKey is required");
    }
    Payment p = new Payment(payerType, payerId, amountMinor, currency, paymentMethod.code(),
        idempotencyKey, notes, Instant.now());
    p.validate();
    return p;
  }

  public static Payment restore(
      long id, String payerType, long payerId, Long referenceId, String referenceType,
      long amountMinor, String currency, String paymentMethodCode, String idempotencyKey,
      PaymentStatus status, PaymentError failureReason, String failureMessage, String notes,
      Instant createdAt, Instant updatedAt, Instant paidAt) {

    Payment payment = new Payment(payerType, payerId, amountMinor, currency, paymentMethodCode,
        idempotencyKey, notes, createdAt);
    payment.id = id;
    payment.status = Objects.requireNonNull(status, "status");
    payment.referenceType = referenceType;
    payment.referenceId = referenceId;
    payment.failureReason = failureReason;
    payment.failureMessage = failureMessage;
    payment.notes = notes;
    payment.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    payment.paidAt = paidAt;
    payment.validate();
    return payment;
  }

  public void markPaid(String referenceType, long referenceId) {
    require(PaymentStatus.PAID);
    if (referenceType == null || referenceType.isBlank()) throw new IllegalArgumentException("referenceType is required");
    if (referenceId < 0) throw new IllegalArgumentException("referenceId must be non-negative");
    if (referenceId == 0 && !"NONE".equals(referenceType)) {
      throw new IllegalArgumentException("referenceId 0 is only allowed with referenceType NONE");
    }
    this.status = PaymentStatus.PAID;
    this.referenceType = referenceType;
    this.referenceId = referenceId;
    this.paidAt = Instant.now();
    this.updatedAt = this.paidAt;
    validate();
  }

  /** Attaches the settled business record to a payment that is already marked paid. */
  public void attachReference(String referenceType, long referenceId) {
    if (status != PaymentStatus.PAID) {
      throw new PaymentException(PaymentError.INVALID_STATE, id > 0 ? Long.valueOf(id) : null,
          "cannot attach a reference to a " + status + " payment", null);
    }
    if (referenceType == null || referenceType.isBlank()) {
      throw new IllegalArgumentException("referenceType is required");
    }
    if (referenceId < 0) throw new IllegalArgumentException("referenceId must be non-negative");
    if (referenceId == 0 && !"NONE".equals(referenceType)) {
      throw new IllegalArgumentException("referenceId 0 is only allowed with referenceType NONE");
    }
    this.referenceType = referenceType;
    this.referenceId = referenceId;
    this.updatedAt = Instant.now();
    validate();
  }

  public void markFailed(PaymentError reason, String message) {
    require(PaymentStatus.FAILED);
    if (reason == null) throw new IllegalArgumentException("reason is required");
    this.status = PaymentStatus.FAILED;
    this.failureReason = reason;
    this.failureMessage = message;
    this.updatedAt = Instant.now();
    validate();
  }

  private void require(PaymentStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new PaymentException(PaymentError.INVALID_STATE,
          id > 0 ? id : null,
          "cannot transition from " + status + " to " + target, null);
    }
  }

  private void validate() {
    if (id < 0) throw new IllegalStateException("Payment id cannot be negative");
    if (payerType == null || payerType.isBlank()) throw new IllegalStateException("Missing payer type");
    if (payerId <= 0) throw new IllegalStateException("Persisted payment must have a payer");
    if (amountMinor <= 0) throw new IllegalStateException("Persisted payment must have a positive amount");
    if (currency == null || currency.isBlank()) throw new IllegalStateException("Missing currency");
    if (paymentMethodCode == null || paymentMethodCode.isBlank()) throw new IllegalStateException("Missing payment method");
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalStateException("Missing idempotency key");
    }
    if (status != PaymentStatus.FAILED && failureReason != null) {
      throw new IllegalStateException("Only FAILED payments carry a failure reason");
    }
    status.validate(this);
  }

  public long getId() { return id; }
  public void setId(long id) {
    if (id <= 0) throw new IllegalArgumentException("id must be positive");
    this.id = id;
  }
  public String getPayerType() { return payerType; }
  public long getPayerId() { return payerId; }
  public long getAmountMinor() { return amountMinor; }
  public String getCurrency() { return currency; }
  public String getPaymentMethodCode() { return paymentMethodCode; }
  public PaymentMethod getPaymentMethod() { return PaymentMethods.fromCode(paymentMethodCode); }
  public String getIdempotencyKey() { return idempotencyKey; }
  public PaymentStatus getStatus() { return status; }
  public String getReferenceType() { return referenceType; }
  public Long getReferenceId() { return referenceId; }
  public PaymentError getFailureReason() { return failureReason; }
  public String getFailureMessage() { return failureMessage; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Instant getPaidAt() { return paidAt; }

  public boolean isPaid() { return status == PaymentStatus.PAID; }
  public boolean isTerminal() { return status.isTerminal(); }
}