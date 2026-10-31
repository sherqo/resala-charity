package com.resala.persistence.sqlite;

import com.resala.beneficiary.BeneficiaryRepository;
import com.resala.beneficiary.Distribution;
import com.resala.model.Beneficiary;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteBeneficiaryRepository implements BeneficiaryRepository {

  private final Connection connection;

  public SqliteBeneficiaryRepository(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Beneficiary save(Beneficiary b) {
    try {
      if (b.getId() == 0) return insert(b);
      try (
        PreparedStatement ps = connection.prepareStatement(
          """
          UPDATE beneficiaries
          SET name=?, phone=?, household_size=?, need_type_id=?, status=?, notes=?
          WHERE id=?
          """
        )
      ) {
        bind(ps, b);
        ps.setLong(7, b.getId());
        ps.executeUpdate();
        return b;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not save beneficiary", e);
    }
  }

  @Override
  public Optional<Beneficiary> findById(long id) {
    try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM beneficiaries WHERE id=?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(map(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load beneficiary", e);
    }
  }

  @Override
  public List<Beneficiary> findAll() {
    try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM beneficiaries ORDER BY name")) {
      try (ResultSet rs = ps.executeQuery()) {
        List<Beneficiary> list = new ArrayList<>();
        while (rs.next()) list.add(map(rs));
        return list;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list beneficiaries", e);
    }
  }

  @Override
  public void delete(long id) {
    try (PreparedStatement ps = connection.prepareStatement("DELETE FROM beneficiaries WHERE id=?")) {
      ps.setLong(1, id);
      if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Beneficiary not found");
    } catch (SQLException e) {
      throw new IllegalStateException("Could not delete beneficiary", e);
    }
  }

  @Override
  public Distribution saveDistribution(Distribution d) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        """
        INSERT INTO distributions (beneficiary_id, donation_id, item_description, quantity, distribution_date, notes)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        Statement.RETURN_GENERATED_KEYS
      )
    ) {
      ps.setLong(1, d.getBeneficiaryId());
      if (d.getDonationId() != null) {
        ps.setLong(2, d.getDonationId());
      } else {
        ps.setNull(2, Types.INTEGER);
      }
      ps.setString(3, d.getItemDescription());
      ps.setInt(4, d.getQuantity());
      ps.setString(5, d.getDistributionDate().toString());
      ps.setString(6, d.getNotes());
      ps.executeUpdate();

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No distribution id returned");
        d.setId(keys.getLong(1));
        return d;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not record distribution", e);
    }
  }

  @Override
  public List<Distribution> findDistributionsByBeneficiary(long beneficiaryId) {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "SELECT * FROM distributions WHERE beneficiary_id=? ORDER BY distribution_date DESC, id DESC"
      )
    ) {
      ps.setLong(1, beneficiaryId);
      try (ResultSet rs = ps.executeQuery()) {
        List<Distribution> list = new ArrayList<>();
        while (rs.next()) list.add(mapDistribution(rs));
        return list;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list distributions for beneficiary", e);
    }
  }

  @Override
  public List<Distribution> findAllDistributions() {
    try (
      PreparedStatement ps = connection.prepareStatement(
        "SELECT * FROM distributions ORDER BY distribution_date DESC, id DESC"
      )
    ) {
      try (ResultSet rs = ps.executeQuery()) {
        List<Distribution> list = new ArrayList<>();
        while (rs.next()) list.add(mapDistribution(rs));
        return list;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list all distributions", e);
    }
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private Beneficiary insert(Beneficiary b) throws SQLException {
    try (
      PreparedStatement ps = connection.prepareStatement(
        """
        INSERT INTO beneficiaries (name, phone, household_size, need_type_id, status, notes)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        Statement.RETURN_GENERATED_KEYS
      )
    ) {
      bind(ps, b);
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No beneficiary id returned");
        b.setId(keys.getLong(1));
        return b;
      }
    }
  }

  private void bind(PreparedStatement ps, Beneficiary b) throws SQLException {
    ps.setString(1, b.getName());
    ps.setString(2, b.getPhone());
    ps.setInt(3, b.getHouseholdSize());
    ps.setInt(4, b.getNeedTypeId());
    ps.setString(5, b.getStatus().name());
    ps.setString(6, b.getNotes());
  }

  private Beneficiary map(ResultSet rs) throws SQLException {
    return new Beneficiary(
      rs.getLong("id"),
      rs.getString("name"),
      rs.getString("phone"),
      rs.getInt("household_size"),
      rs.getInt("need_type_id"),
      Beneficiary.Status.valueOf(rs.getString("status")),
      rs.getString("notes")
    );
  }

  private Distribution mapDistribution(ResultSet rs) throws SQLException {
    long donationIdVal = rs.getLong("donation_id");
    Long donationId = rs.wasNull() ? null : donationIdVal;

    return new Distribution(
      rs.getLong("id"),
      rs.getLong("beneficiary_id"),
      donationId,
      rs.getString("item_description"),
      rs.getInt("quantity"),
      LocalDate.parse(rs.getString("distribution_date")),
      rs.getString("notes")
    );
  }
}
