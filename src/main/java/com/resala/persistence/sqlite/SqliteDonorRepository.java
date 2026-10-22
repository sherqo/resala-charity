package com.resala.persistence.sqlite;

import com.resala.donation.DonorRepository;
import com.resala.model.Donor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteDonorRepository implements DonorRepository {

  private final Connection connection;

  public SqliteDonorRepository(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Donor save(Donor donor) {
    try {
      if (donor.getId() == 0) return insert(donor);
      try (
        PreparedStatement ps = connection.prepareStatement(
          """
          UPDATE donors SET name=?, type=?, email=?, phone=?, active=? WHERE id=?
          """
        )
      ) {
        bind(ps, donor);
        ps.setLong(7, donor.getId());
        ps.executeUpdate();
        return donor;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not save donor", e);
    }
  }

  @Override
  public Optional<Donor> findById(long id) {
    try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM donors WHERE id=?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Optional.of(map(rs)) : Optional.empty();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not load donor", e);
    }
  }

  @Override
  public List<Donor> findActive(String query) {
    String sql = "SELECT * FROM donors WHERE active=1";
    boolean hasQuery = query != null && !query.isBlank();
    if (hasQuery) sql += " AND (LOWER(name) LIKE ? OR LOWER(COALESCE(email, '')) LIKE ? OR phone LIKE ?)";
    sql += " ORDER BY name";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      if (hasQuery) {
        String like = "%" + query.trim().toLowerCase() + "%";
        ps.setString(1, like);
        ps.setString(2, like);
        ps.setString(3, like);
      }
      try (ResultSet rs = ps.executeQuery()) {
        List<Donor> donors = new ArrayList<>();
        while (rs.next()) donors.add(map(rs));
        return donors;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Could not list donors", e);
    }
  }

  @Override
  public void archive(long id) {
    try (PreparedStatement ps = connection.prepareStatement("UPDATE donors SET active=0 WHERE id=?")) {
      ps.setLong(1, id);
      if (ps.executeUpdate() == 0) throw new IllegalArgumentException("Donor not found");
    } catch (SQLException e) {
      throw new IllegalStateException("Could not archive donor", e);
    }
  }

  private Donor insert(Donor donor) throws SQLException {
    try (
      PreparedStatement ps = connection.prepareStatement(
        """
        INSERT INTO donors (name, type, email, phone, active) VALUES (?, ?, ?, ?, ?)
        """,
        Statement.RETURN_GENERATED_KEYS
      )
    ) {
      bind(ps, donor);
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No donor id returned");
        donor.setId(keys.getLong(1));
        return donor;
      }
    }
  }

  private void bind(PreparedStatement ps, Donor donor) throws SQLException {
    ps.setString(1, donor.getName());
    ps.setString(2, donor.getType().name());
    ps.setString(3, donor.getEmail());
    ps.setString(4, donor.getPhone());
    ps.setInt(5, donor.isActive() ? 1 : 0);
  }

  private Donor map(ResultSet rs) throws SQLException {
    return new Donor(
      rs.getLong("id"),
      rs.getString("name"),
      Donor.Type.valueOf(rs.getString("type")),
      rs.getString("email"),
      rs.getString("phone"),
      rs.getInt("active") == 1
    );
  }
}
