package com.resala.persistence.sqlite;

import com.resala.donation.Donation;
import com.resala.donation.DonationFilter;
import com.resala.donation.DonationRepository;
import com.resala.donation.DonationRequest;
import com.resala.model.DonationType;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteDonationRepository implements DonationRepository {

  private final Connection connection;

  public SqliteDonationRepository(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Donation save(Donation donation) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        """
        INSERT INTO donations (donor_id, donation_type, amount_minor, item_description, quantity,
          estimated_value_minor, reference_number, donation_date, notes)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        Statement.RETURN_GENERATED_KEYS
      )
    ) {
      ps.setLong(1, donation.getDonorId());
      ps.setString(2, donation.getType().name());
      ps.setLong(3, donation.getAmountMinor());
      ps.setString(4, donation.getItemDescription());
      if (donation.getQuantity() == null) ps.setNull(5, java.sql.Types.INTEGER);
      else ps.setInt(5, donation.getQuantity());
      if (donation.getEstimatedValueMinor() == null) ps.setNull(6, java.sql.Types.INTEGER);
      else ps.setLong(6, donation.getEstimatedValueMinor());
      ps.setString(7, donation.getReferenceNumber());
      ps.setString(8, donation.getDonationDate().toString());
      ps.setString(9, donation.getNotes());
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No donation id returned");
        donation.setId(keys.getLong(1));
        return donation;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not save donation", e);
    }
  }

  @Override
  public Optional<Donation> findById(long id) {
    List<Donation> results = find(" WHERE id=?", List.of(id));
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
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
      values.add(Date.valueOf(filter.from()));
    }
    if (filter.to() != null) {
      where.append(" AND donation_date<=?");
      values.add(Date.valueOf(filter.to()));
    }
    return find(where.toString(), values);
  }

  @Override
  public long monetaryTotalForDonorYear(long donorId, int year) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        """
        SELECT COALESCE(SUM(amount_minor), 0) FROM donations
        WHERE donor_id=? AND donation_type IN ('ONLINE', 'CHECK') AND strftime('%Y', donation_date)=?
        """
      )
    ) {
      ps.setLong(1, donorId);
      ps.setString(2, String.valueOf(year));
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not calculate donor total", e);
    }
  }

  private List<Donation> find(String where, List<Object> values) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "SELECT * FROM donations" + where + " ORDER BY donation_date DESC, id DESC"
      )
    ) {
      for (int i = 0; i < values.size(); i++) ps.setObject(i + 1, values.get(i));
      try (ResultSet rs = ps.executeQuery()) {
        List<Donation> donations = new ArrayList<>();
        while (rs.next()) donations.add(map(rs));
        return donations;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list donations", e);
    }
  }

  private Donation map(ResultSet rs) throws SQLException {
    DonationType type = DonationType.valueOf(rs.getString("donation_type"));
    Integer quantity = nullableInteger(rs, "quantity");
    Long estimated = nullableLong(rs, "estimated_value_minor");
    DonationRequest request = new DonationRequest(
      rs.getLong("donor_id"),
      type,
      rs.getLong("amount_minor"),
      rs.getString("item_description"),
      quantity,
      estimated,
      rs.getString("reference_number"),
      java.time.LocalDate.parse(rs.getString("donation_date")),
      rs.getString("notes")
    );
    Donation donation = new com.resala.donation.DonationFactory().create(request);
    donation.setId(rs.getLong("id"));
    return donation;
  }

  private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }

  private Long nullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }
}
