package com.resala.payment;

public enum PaymentStatus {
  PENDING,
  PAID,
  REFUNDED,
  FAILED;


  public boolean isTerminal() {
    return this != PENDING;
  }

  public boolean canTransitionTo(PaymentStatus next) {
  return switch (this) {
      case PENDING -> next == PAID || next == FAILED;
      case PAID -> next == REFUNDED;
      case FAILED, REFUNDED -> false;
    };
  }
}
