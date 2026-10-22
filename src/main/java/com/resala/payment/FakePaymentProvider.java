package com.resala.payment;

import java.util.UUID;

// mock for an external payment gateway
public class FakePaymentProvider {

  public String makePayment(long cents) {
    return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }
}
