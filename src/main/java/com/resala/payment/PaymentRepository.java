package com.resala.payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
  Payment save(Payment payment);
  Optional<Payment> findById(long id);
  Optional<Payment> findByIdempotencyKey(String key);

  default List<Payment> findAll() {
    return List.of();
  }

  default Optional<Payment> findByReference(String reference) {
    return findByIdempotencyKey(reference);
  }
}
