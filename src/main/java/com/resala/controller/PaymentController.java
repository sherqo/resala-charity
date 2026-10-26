package com.resala.controller;

import com.resala.payment.PaymentMethod;
import com.resala.payment.PaymentOutcome;
import com.resala.payment.PaymentRequest;
import com.resala.payment.PaymentService;
import java.util.List;

public class PaymentController {

  private final PaymentService payments;

  public PaymentController(PaymentService payments) {
    this.payments = payments;
  }

  public List<PaymentMethod> availableMethods() {
    return payments.availableMethods();
  }

  public PaymentOutcome payDonation(
      long donorId, long amountMinor, PaymentMethod method, String idempotencyKey, String notes) {
    return payments.pay(new PaymentRequest(
        idempotencyKey, "DONOR", donorId, amountMinor, "EGP", method, "Online donation", notes));
  }
}
