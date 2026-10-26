package com.resala.payment;

public record PaymentCommand(
    String idempotencyKey,
    long amountMinor,
    String currency,
    PaymentMethod paymentMethod,
    String description
) {}