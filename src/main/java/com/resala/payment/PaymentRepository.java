package com.resala.payment;

import java.util.Optional;
import java.util.List;


public interface PaymentRepository {
  Payment save(Payment payment);
  Optional<Payment> findById(long id);
  //TODO: i think i will remove this but leave it till now
  Optional<Payment> findByIdempotencyKey(String key);
  List<Payment> findByDonor(long donorId);
  List<Payment> findByStatus(PaymentStatus status);
}

