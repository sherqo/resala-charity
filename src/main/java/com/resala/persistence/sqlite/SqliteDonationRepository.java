package com.resala.persistence.sqlite;

import com.resala.core.JdbcRepository;
import com.resala.donation.Donation;
import com.resala.donation.DonationFilter;
import com.resala.donation.DonationRepository;
import com.resala.donation.DonationRequest;
import com.resala.donation.DonationStatus;
import com.resala.model.DonationType;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteDonationRepository extends JdbcRepository implements DonationRepository {

  private static final JdbcRepository.RowMapper<Donation> DONATION_MAPPER = rs -> {
    DonationType type = DonationType.valueOf(str(rs, "donation_type"));
    Integer quantity = nullableInteger(rs, "quantity");
    Long estimated = nullableLong(rs, "estimated_value_minor");
    DonationRequest request = new DonationRequest(
      lng(rs, "donor_id"),
      type,
      lng(rs, "amount_minor"),
      str(rs, "item_description"),
      quantity,
      estimated,
      str(rs, "reference_number"),
      LocalDate.parse(str(rs, "donation_date")),
      str(rs, "notes")
    );
    Donation donation = new com.resala.donation.DonationFactory().create(request);
    donation.setId(lng(rs, "id"));
    donation.setStatus(DonationStatus.valueOf(str(rs, "status")));
    return donation;
  };

  public SqliteDonationRepository(Connection connection) {
    super(connection);
  }

  @Override
  public Donation save(Donation donation) {
    long id = insertReturnKey(
      "INSERT INTO donations (donor_id, donation_type, amount_minor, item_description, quantity, estimated_value_minor, reference_number, donation_date, notes, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
      donation.getDonorId(), donation.getType().name(), donation.getAmountMinor(),
      donation.getItemDescription(), donation.getQuantity(), donation.getEstimatedValueMinor(),
      donation.getReferenceNumber(), donation.getDonationDate().toString(), donation.getNotes(),
      donation.getStatus().name()
    );
    donation.setId(id);
    return donation;
  }

  @Override
  public Optional<Donation> findById(long id) {
    return queryOne("SELECT * FROM donations WHERE id=?", DONATION_MAPPER, id);
  }

  @Override
  public List<Donation> find(DonationFilter filter) {
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    List<Object> values = new ArrayList<>();
    if (filter.donorId() != null) {
      where.append(" AND donor_id=?");
      values.add(filter.donorId());
    }
    if (filter.type() != null) {
      where.append(" AND donation_type=?");
      values.add(filter.type().name());
    }
    if (filter.from() != null) {
      where.append(" AND donation_date>=?");
      values.add(java.sql.Date.valueOf(filter.from()));
    }
    if (filter.to() != null) {
      where.append(" AND donation_date<=?");
      values.add(java.sql.Date.valueOf(filter.to()));
    }
    return query("SELECT * FROM donations" + where + " ORDER BY donation_date DESC, id DESC",
      DONATION_MAPPER, values.toArray());
  }

  @Override
  public void delete(long id) {
    int deleted = execute("DELETE FROM donations WHERE id=?", id);
    if (deleted == 0) throw new IllegalArgumentException("Donation not found");
  }

  @Override
  public Donation update(DonationRequest request, long donationId) {
    int updated = execute(
      "UPDATE donations SET amount_minor=?, reference_number=?, donation_date=?, notes=? WHERE id=?",
      request.amountMinor(), request.referenceNumber(), request.donationDate().toString(),
      request.notes(), donationId
    );
    if (updated == 0) throw new IllegalArgumentException("Donation not found");
    return findById(donationId)
        .orElseThrow(() -> new IllegalArgumentException("Donation not found after update"));
  }

  @Override
  public long monetaryTotalForDonorYear(long donorId, int year) {
    List<Long> rows = query(
      "SELECT COALESCE(SUM(amount_minor), 0) FROM donations WHERE donor_id=? AND donation_type IN ('ONLINE', 'CHECK') AND strftime('%Y', donation_date)=?",
      rs -> rs.getLong(1), donorId, String.valueOf(year));
    return rows.isEmpty() ? 0 : rows.get(0);
  }

  @Override
  public Optional<Donation> findByReferenceNumber(String referenceNumber) {
    if (referenceNumber == null || referenceNumber.isBlank()) return Optional.empty();
    return query("SELECT * FROM donations WHERE reference_number=? ORDER BY donation_date DESC, id DESC",
      DONATION_MAPPER, referenceNumber).stream().findFirst();
  }

  private static Integer nullableInteger(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }

  private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }
}
