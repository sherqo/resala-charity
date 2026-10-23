package com.resala.payment;

public record GatewayResult(
    boolean approved,
    long amountMinor,
    String currency,
    String message,
    PaymentError failureReason
) {
  public GatewayResult {
    if (approved && failureReason != null) throw new IllegalArgumentException("approved result cannot carry a failure reason");
    if (!approved && failureReason == null) throw new IllegalArgumentException("failed result must carry a failure reason");
  }
  public static GatewayResult approved(long amount, String currency, String message) {
    return new GatewayResult(true, amount, currency, message, null);
  }
  public static GatewayResult failed(PaymentError reason, String message, long amount, String currency) {
    return new GatewayResult(false, amount, currency, message, reason);
  }
}