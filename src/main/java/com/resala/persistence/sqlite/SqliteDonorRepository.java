package com.resala.persistence.sqlite;

import com.resala.core.JdbcRepository;
import com.resala.donation.DonorRepository;
import com.resala.model.Donor;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class SqliteDonorRepository extends JdbcRepository implements DonorRepository {

  private static final JdbcRepository.RowMapper<Donor> DONOR_MAPPER = rs -> new Donor(
    lng(rs, "id"),
    str(rs, "name"),
    Donor.Type.valueOf(str(rs, "type")),
    str(rs, "email"),
    str(rs, "phone"),
    num(rs, "active") == 1
  );

  public SqliteDonorRepository(Connection connection) {
    super(connection);
  }

  @Override
  public Donor save(Donor donor) {
    if (donor.getId() == 0) {
      long id = insertReturnKey(
        "INSERT INTO donors (name, type, email, phone, active) VALUES (?, ?, ?, ?, ?)",
        donor.getName(), donor.getType().name(), donor.getEmail(),
        donor.getPhone(), donor.isActive() ? 1 : 0
      );
      donor.setId(id);
      return donor;
    }
    int updated = execute(
      "UPDATE donors SET name=?, type=?, email=?, phone=?, active=? WHERE id=?",
      donor.getName(), donor.getType().name(), donor.getEmail(),
      donor.getPhone(), donor.isActive() ? 1 : 0, donor.getId()
    );
    if (updated == 0) throw new IllegalArgumentException("Donor not found");
    return donor;
  }

  @Override
  public Optional<Donor> findById(long id) {
    return queryOne("SELECT * FROM donors WHERE id=?", DONOR_MAPPER, id);
  }

  @Override
  public List<Donor> findActive(String query) {
    String sql = "SELECT * FROM donors WHERE active=1";
    boolean hasQuery = query != null && !query.isBlank();
    if (hasQuery) sql += " AND (LOWER(name) LIKE ? OR LOWER(COALESCE(email, '')) LIKE ? OR phone LIKE ?)";
    sql += " ORDER BY name";
    if (hasQuery) {
      String like = "%" + query.trim().toLowerCase() + "%";
      return query(sql, DONOR_MAPPER, like, like, like);
    }
    return query(sql, DONOR_MAPPER);
  }

  @Override
  public void archive(long id) {
    int updated = execute("UPDATE donors SET active=0 WHERE id=?", id);
    if (updated == 0) throw new IllegalArgumentException("Donor not found");
  }
}
