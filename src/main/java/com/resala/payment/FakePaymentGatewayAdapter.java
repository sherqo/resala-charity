package com.resala.payment;

import java.util.List;

public class FakePaymentGatewayAdapter implements PaymentGateway {

  private final FakePaymentProvider provider;

  public FakePaymentGatewayAdapter(FakePaymentProvider provider) {
    this.provider = provider;
  }

  @Override
  public List<PaymentMethod> getAvailableMethods() {
    return provider.availableMethods();
  }

  @Override
  public GatewayResult charge(PaymentCommand cmd) {
    try {
      FakePaymentProvider.ProviderTxn txn = provider.makePayment(
          cmd.idempotencyKey(), cmd.amountMinor(), cmd.paymentMethod(), cmd.currency());

      return switch (txn.statusCode()) {
        case "00" -> GatewayResult.approved(txn.amountCents(), txn.currency(), txn.message());
        case "51", "54" -> GatewayResult.failed(PaymentError.PROVIDER_DECLINED, txn.message(),
            txn.amountCents(), txn.currency());
        default -> GatewayResult.failed(PaymentError.PROVIDER_UNAVAILABLE, txn.message(),
            txn.amountCents(), txn.currency());
      };
    } catch (FakePaymentProvider.ProviderException e) {
      return switch (e.error) {
        case TIMEOUT -> GatewayResult.failed(PaymentError.PROVIDER_TIMEOUT, e.getMessage(),
            cmd.amountMinor(), cmd.currency());
        case NETWORK -> GatewayResult.failed(PaymentError.PROVIDER_UNAVAILABLE, e.getMessage(),
            cmd.amountMinor(), cmd.currency());
      };
    }
  }
}