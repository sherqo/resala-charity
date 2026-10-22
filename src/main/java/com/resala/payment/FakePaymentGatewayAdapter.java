package com.resala.payment;

// adapter pattern for the payment gateway
public class FakePaymentGatewayAdapter implements PaymentGateway {

  private final FakePaymentProvider provider;

  public FakePaymentGatewayAdapter(FakePaymentProvider provider) {
    this.provider = provider;
  }

  public String charge(long amountMinor) {
    return provider.makePayment(amountMinor);
  }
}
