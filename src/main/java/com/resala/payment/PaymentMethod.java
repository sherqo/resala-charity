package com.resala.payment;

public interface PaymentMethod {
  String code();
  String displayName();
  double feeRate();
  void validate(PaymentCommand cmd);
}