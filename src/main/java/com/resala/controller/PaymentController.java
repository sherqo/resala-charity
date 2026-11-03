package com.resala.controller;

import com.resala.payment.Payment;
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

  /**
   * Charges a donation through the payment gateway.
   *
   * @param paymentToken one-time token produced from the card/wallet details; null for cash
   * @param idempotencyKey caller reference; reusing it resumes the same payment
   */
  public PaymentOutcome payDonation(
      long donorId, long amountMinor, PaymentMethod method, String paymentToken,
      String idempotencyKey, String notes) {
    return payments.pay(new PaymentRequest(
        idempotencyKey, "DONOR", donorId, amountMinor, "EGP", method, paymentToken,
        "Online donation", notes));
  }

  public Payment getPayment(long paymentId) {
    return payments.get(paymentId);
  }

  public List<Payment> listPayments() {
    return payments.listPayments();
  }
}
