package com.resala.payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
  Payment save(Payment payment);
  Optional<Payment> findById(long id);
  Optional<Payment> findByIdempotencyKey(String key);
  List<Payment> findByPayer(String payerType, long payerId);
  List<Payment> findByStatus(PaymentStatus status);
}