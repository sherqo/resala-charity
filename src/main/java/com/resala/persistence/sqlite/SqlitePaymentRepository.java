package com.resala.persistence.sqlite;

import com.resala.payment.Payment;
import com.resala.payment.PaymentError;
import com.resala.payment.PaymentMethod;
import com.resala.payment.PaymentMethods;
import com.resala.payment.PaymentRepository;
import com.resala.payment.PaymentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlitePaymentRepository implements PaymentRepository {

  private final Connection connection;

  public SqlitePaymentRepository(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Payment save(Payment payment) {
    try {
      if (payment.getId() == 0) return insert(payment);
      update(payment);
      return payment;
    } catch (SQLException e) {
      throw new IllegalStateException("Could not save payment", e);
    }
  }

  @Override
  public Optional<Payment> findById(long id) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT p.*, pm.name AS method_name FROM payments p JOIN payment_methods pm ON p.payment_method_id = pm.id WHERE p.id=?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(map(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load payment", e);
    }
  }

  @Override
  public Optional<Payment> findByIdempotencyKey(String key) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT p.*, pm.name AS method_name FROM payments p JOIN payment_methods pm ON p.payment_method_id = pm.id WHERE p.idempotency_key=?")) {
      ps.setString(1, key);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(map(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load payment", e);
    }
  }

  @Override
  public List<Payment> findByPayer(String payerType, long payerId) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT p.*, pm.name AS method_name FROM payments p JOIN payment_methods pm ON p.payment_method_id = pm.id WHERE p.payer_type=? AND p.payer_id=? ORDER BY p.created_at DESC")) {
      ps.setString(1, payerType);
      ps.setLong(2, payerId);
      try (ResultSet rs = ps.executeQuery()) {
        List<Payment> payments = new ArrayList<>();
        while (rs.next()) payments.add(map(rs));
        return payments;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list payments", e);
    }
  }

  @Override
  public List<Payment> findByStatus(PaymentStatus status) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT p.*, pm.name AS method_name FROM payments p JOIN payment_methods pm ON p.payment_method_id = pm.id WHERE p.status=? ORDER BY p.created_at")) {
      ps.setString(1, status.name());
      try (ResultSet rs = ps.executeQuery()) {
        List<Payment> payments = new ArrayList<>();
        while (rs.next()) payments.add(map(rs));
        return payments;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list payments", e);
    }
  }

  private Payment insert(Payment payment) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement(
        """
        INSERT INTO payments (payer_type, payer_id, amount_minor, currency, payment_method_id,
          status, idempotency_key, failure_code, failure_message, notes, reference_type, reference_id,
          created_at, updated_at, paid_at)
        VALUES (?, ?, ?, ?, (SELECT id FROM payment_methods WHERE name=?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        Statement.RETURN_GENERATED_KEYS)) {
      bindInsert(ps, payment);
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No payment id returned");
        payment.setId(keys.getLong(1));
        return payment;
      }
    }
  }

  private void update(Payment payment) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement(
        """
        UPDATE payments SET payer_type=?, payer_id=?, amount_minor=?, currency=?, payment_method_id=(SELECT id FROM payment_methods WHERE name=?),
          status=?, idempotency_key=?, failure_code=?, failure_message=?, notes=?,
          reference_type=?, reference_id=?, created_at=?, updated_at=?, paid_at=?
        WHERE id=?
        """)) {
      int i = 1;
      ps.setString(i++, payment.getPayerType());
      ps.setLong(i++, payment.getPayerId());
      ps.setLong(i++, payment.getAmountMinor());
      ps.setString(i++, payment.getCurrency());
      ps.setString(i++, payment.getPaymentMethod().code());
      ps.setString(i++, payment.getStatus().name());
      ps.setString(i++, payment.getIdempotencyKey());
      if (payment.getFailureReason() == null) ps.setNull(i++, java.sql.Types.VARCHAR);
      else ps.setString(i++, payment.getFailureReason().name());
      ps.setString(i++, payment.getFailureMessage());
      ps.setString(i++, payment.getNotes());
      if (payment.getReferenceType() == null) ps.setNull(i++, java.sql.Types.VARCHAR);
      else ps.setString(i++, payment.getReferenceType());
      if (payment.getReferenceId() == null) ps.setNull(i++, java.sql.Types.INTEGER);
      else ps.setLong(i++, payment.getReferenceId());
      ps.setString(i++, payment.getCreatedAt().toString());
      ps.setString(i++, payment.getUpdatedAt().toString());
      if (payment.getPaidAt() == null) ps.setNull(i++, java.sql.Types.VARCHAR);
      else ps.setString(i++, payment.getPaidAt().toString());
      ps.setLong(i, payment.getId());
      ps.executeUpdate();
    }
  }

  private void bindInsert(PreparedStatement ps, Payment payment) throws SQLException {
    int i = 1;
    ps.setString(i++, payment.getPayerType());
    ps.setLong(i++, payment.getPayerId());
    ps.setLong(i++, payment.getAmountMinor());
    ps.setString(i++, payment.getCurrency());
    ps.setString(i++, payment.getPaymentMethod().code());
    ps.setString(i++, payment.getStatus().name());
    ps.setString(i++, payment.getIdempotencyKey());
    if (payment.getFailureReason() == null) ps.setNull(i++, java.sql.Types.VARCHAR);
    else ps.setString(i++, payment.getFailureReason().name());
    ps.setString(i++, payment.getFailureMessage());
    ps.setString(i++, payment.getNotes());
    if (payment.getReferenceType() == null) ps.setNull(i++, java.sql.Types.VARCHAR);
    else ps.setString(i++, payment.getReferenceType());
    if (payment.getReferenceId() == null) ps.setNull(i++, java.sql.Types.INTEGER);
    else ps.setLong(i++, payment.getReferenceId());
    ps.setString(i++, payment.getCreatedAt().toString());
    ps.setString(i++, payment.getUpdatedAt().toString());
    if (payment.getPaidAt() == null) ps.setNull(i++, java.sql.Types.VARCHAR);
    else ps.setString(i++, payment.getPaidAt().toString());
  }

  private Payment map(ResultSet rs) throws SQLException {
    PaymentError failure = nullableError(rs.getString("failure_code"));
    return Payment.restore(
        rs.getLong("id"),
        rs.getString("payer_type"),
        rs.getLong("payer_id"),
        nullableLong(rs, "reference_id"),
        rs.getString("reference_type"),
        rs.getLong("amount_minor"),
        rs.getString("currency"),
        rs.getString("method_name"),
        rs.getString("idempotency_key"),
        PaymentStatus.valueOf(rs.getString("status")),
        failure,
        rs.getString("failure_message"),
        rs.getString("notes"),
        Instant.parse(rs.getString("created_at")),
        Instant.parse(rs.getString("updated_at")),
        nullableInstant(rs, "paid_at")
    );
  }

  private Long nullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  private Instant nullableInstant(ResultSet rs, String column) throws SQLException {
    String value = rs.getString(column);
    return value == null ? null : Instant.parse(value);
  }

  private PaymentError nullableError(String code) {
    return code == null ? null : PaymentError.valueOf(code);
  }
}