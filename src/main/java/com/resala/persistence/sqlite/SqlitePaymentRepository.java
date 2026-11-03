package com.resala.persistence.sqlite;

import com.resala.core.JdbcRepository;
import com.resala.payment.Payment;
import com.resala.payment.PaymentError;
import com.resala.payment.PaymentMethod;
import com.resala.payment.PaymentMethods;
import com.resala.payment.PaymentRepository;
import com.resala.payment.PaymentStatus;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class SqlitePaymentRepository extends JdbcRepository implements PaymentRepository {

  public SqlitePaymentRepository(Connection connection) {
    super(connection);
  }

  @Override
  public Payment save(Payment payment) {
    try {
      if (payment.getId() == 0) return insert(payment);
      update(payment);
      return payment;
    } catch (java.sql.SQLException e) {
      throw new IllegalStateException("Could not save payment", e);
    }
  }

  @Override
  public Optional<Payment> findById(long id) {
    return queryOne(
      "SELECT p.*, pm.name AS method_name FROM payments p JOIN payment_methods pm ON p.payment_method_id = pm.id WHERE p.id=?",
      PAYMENT_MAPPER, id);
  }

  @Override
  public Optional<Payment> findByIdempotencyKey(String key) {
    return queryOne(
      "SELECT p.*, pm.name AS method_name FROM payments p JOIN payment_methods pm ON p.payment_method_id = pm.id WHERE p.idempotency_key=?",
      PAYMENT_MAPPER, key);
  }

  @Override
  public List<Payment> findByPayer(String payerType, long payerId) {
    return query(
      "SELECT p.*, pm.name AS method_name FROM payments p JOIN payment_methods pm ON p.payment_method_id = pm.id WHERE p.payer_type=? AND p.payer_id=? ORDER BY p.created_at DESC",
      PAYMENT_MAPPER, payerType, payerId);
  }

  @Override
  public List<Payment> findByStatus(PaymentStatus status) {
    return query(
      "SELECT p.*, pm.name AS method_name FROM payments p JOIN payment_methods pm ON p.payment_method_id = pm.id WHERE p.status=? ORDER BY p.created_at",
      PAYMENT_MAPPER, status.name());
  }

  @Override
  public List<Payment> findAll() {
    return query(
      "SELECT p.*, pm.name AS method_name FROM payments p JOIN payment_methods pm ON p.payment_method_id = pm.id ORDER BY p.id DESC",
      PAYMENT_MAPPER);
  }

  private static final JdbcRepository.RowMapper<Payment> PAYMENT_MAPPER = rs -> {
    PaymentError failure = nullableError(str(rs, "failure_code"));
    return Payment.restore(
      lng(rs, "id"),
      str(rs, "payer_type"),
      lng(rs, "payer_id"),
      nullableLong(rs, "reference_id"),
      str(rs, "reference_type"),
      lng(rs, "amount_minor"),
      lng(rs, "fee_minor"),
      str(rs, "currency"),
      str(rs, "method_name"),
      str(rs, "idempotency_key"),
      PaymentStatus.valueOf(str(rs, "status")),
      failure,
      str(rs, "failure_message"),
      str(rs, "notes"),
      Instant.parse(str(rs, "created_at")),
      Instant.parse(str(rs, "updated_at")),
      nullableInstant(rs, "paid_at")
    );
  };

  private Payment insert(Payment payment) throws java.sql.SQLException {
    long id = insertReturnKey(
      """
      INSERT INTO payments (payer_type, payer_id, amount_minor, fee_minor, currency, payment_method_id,
        status, idempotency_key, failure_code, failure_message, notes, reference_type, reference_id,
        created_at, updated_at, paid_at)
      VALUES (?, ?, ?, ?, ?, (SELECT id FROM payment_methods WHERE name=?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """,
      payment.getPayerType(), payment.getPayerId(), payment.getAmountMinor(), payment.getFeeMinor(),
      payment.getCurrency(), payment.getPaymentMethod().code(), payment.getStatus().name(),
      payment.getIdempotencyKey(),
      payment.getFailureReason() == null ? null : payment.getFailureReason().name(),
      payment.getFailureMessage(), payment.getNotes(),
      payment.getReferenceType(), payment.getReferenceId(),
      payment.getCreatedAt().toString(), payment.getUpdatedAt().toString(),
      payment.getPaidAt() == null ? null : payment.getPaidAt().toString()
    );
    payment.setId(id);
    return payment;
  }

  private void update(Payment payment) throws java.sql.SQLException {
    int updated = execute(
      """
      UPDATE payments SET payer_type=?, payer_id=?, amount_minor=?, fee_minor=?, currency=?,
        payment_method_id=(SELECT id FROM payment_methods WHERE name=?), status=?,
        idempotency_key=?, failure_code=?, failure_message=?, notes=?,
        reference_type=?, reference_id=?, created_at=?, updated_at=?, paid_at=? WHERE id=?
      """,
      payment.getPayerType(), payment.getPayerId(), payment.getAmountMinor(), payment.getFeeMinor(),
      payment.getCurrency(), payment.getPaymentMethod().code(), payment.getStatus().name(),
      payment.getIdempotencyKey(),
      payment.getFailureReason() == null ? null : payment.getFailureReason().name(),
      payment.getFailureMessage(), payment.getNotes(),
      payment.getReferenceType(), payment.getReferenceId(),
      payment.getCreatedAt().toString(), payment.getUpdatedAt().toString(),
      payment.getPaidAt() == null ? null : payment.getPaidAt().toString(),
      payment.getId()
    );
    if (updated == 0) throw new IllegalArgumentException("Payment not found");
  }

  private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  private static Instant nullableInstant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    String value = str(rs, column);
    return value == null ? null : Instant.parse(value);
  }

  private static PaymentError nullableError(String code) {
    return code == null ? null : PaymentError.valueOf(code);
  }
}
