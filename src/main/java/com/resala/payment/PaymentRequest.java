package com.resala.payment;

public record PaymentRequest(
    String idempotencyKey,
    String payerType,
    long payerId,
    long amountMinor,
    String currency,
    PaymentMethod paymentMethod,
    String description,
    String notes
) {}