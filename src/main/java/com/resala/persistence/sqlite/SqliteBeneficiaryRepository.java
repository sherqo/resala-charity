package com.resala.persistence.sqlite;

import com.resala.core.JdbcRepository;
import com.resala.beneficiary.BeneficiaryRepository;
import com.resala.beneficiary.Distribution;
import com.resala.model.Beneficiary;
import com.resala.model.NeedType;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SqliteBeneficiaryRepository extends JdbcRepository implements BeneficiaryRepository {

  private static final JdbcRepository.RowMapper<Beneficiary> BENEFICIARY_MAPPER = rs -> new Beneficiary(
    lng(rs, "id"),
    str(rs, "name"),
    str(rs, "phone"),
    num(rs, "household_size"),
    num(rs, "need_type_id"),
    Beneficiary.Status.valueOf(str(rs, "status")),
    str(rs, "notes"),
    num(rs, "impact_score")
  );

  private static final JdbcRepository.RowMapper<Distribution> DISTRIBUTION_MAPPER = rs -> {
    long donationIdVal = lng(rs, "donation_id");
    Long donationId = rs.wasNull() ? null : donationIdVal;
    return new Distribution(
      lng(rs, "id"),
      lng(rs, "beneficiary_id"),
      donationId,
      str(rs, "item_description"),
      num(rs, "quantity"),
      LocalDate.parse(str(rs, "distribution_date")),
      str(rs, "notes")
    );
  };

  public SqliteBeneficiaryRepository(Connection connection) {
    super(connection);
  }

  @Override
  public Beneficiary save(Beneficiary b) {
    if (b.getId() == 0) {
      try {
        return insert(b);
      } catch (java.sql.SQLException e) {
        throw new IllegalStateException("Could not insert beneficiary", e);
      }
    }
    int updated = execute(
      "UPDATE beneficiaries SET name=?, phone=?, household_size=?, need_type_id=?, status=?, notes=?, impact_score=? WHERE id=?",
      b.getName(), b.getPhone(), b.getHouseholdSize(), b.getNeedTypeId(),
      b.getStatus().name(), b.getNotes(), b.getImpactScore(), b.getId()
    );
    if (updated == 0) throw new IllegalArgumentException("Beneficiary not found");
    return b;
  }

  @Override
  public Optional<Beneficiary> findById(long id) {
    return queryOne("SELECT * FROM beneficiaries WHERE id=?", BENEFICIARY_MAPPER, id);
  }

  @Override
  public List<Beneficiary> findAll() {
    return query("SELECT * FROM beneficiaries ORDER BY name", BENEFICIARY_MAPPER);
  }

  @Override
  public void delete(long id) {
    try {
      int deleted = execute("DELETE FROM beneficiaries WHERE id=?", id);
      if (deleted == 0) throw new IllegalArgumentException("Beneficiary not found");
    } catch (IllegalStateException e) {
      if (e.getMessage() != null && e.getMessage().contains("FOREIGN KEY")) {
        throw new IllegalStateException(
          "Cannot delete beneficiary #" + id + " because they have recorded aid distributions. Please set status to CLOSED instead.");
      }
      throw new IllegalStateException("Could not delete beneficiary: " + e.getMessage(), e);
    }
  }

  @Override
  public Distribution saveDistribution(Distribution d) {
    long id = insertReturnKey(
      "INSERT INTO distributions (beneficiary_id, donation_id, item_description, quantity, distribution_date, notes) VALUES (?, ?, ?, ?, ?, ?)",
      d.getBeneficiaryId(), d.getDonationId(), d.getItemDescription(), d.getQuantity(),
      d.getDistributionDate().toString(), d.getNotes()
    );
    d.setId(id);
    return d;
  }

  @Override
  public List<Distribution> findDistributionsByBeneficiary(long beneficiaryId) {
    return query("SELECT * FROM distributions WHERE beneficiary_id=? ORDER BY distribution_date DESC, id DESC",
      DISTRIBUTION_MAPPER, beneficiaryId);
  }

  @Override
  public List<Distribution> findAllDistributions() {
    return query("SELECT * FROM distributions ORDER BY distribution_date DESC, id DESC", DISTRIBUTION_MAPPER);
  }

  @Override
  public List<NeedType> findAllNeedTypes() {
    return query("SELECT * FROM need_types ORDER BY id",
      rs -> new NeedType(num(rs, "id"), str(rs, "name")));
  }

  @Override
  public Optional<NeedType> findNeedTypeById(int id) {
    return queryOne("SELECT * FROM need_types WHERE id=?",
      rs -> new NeedType(num(rs, "id"), str(rs, "name")), id);
  }

  @Override
  public NeedType saveNeedType(NeedType nt) {
    if (nt.getId() <= 0) {
      long id = insertReturnKey("INSERT INTO need_types (name) VALUES (?)", nt.getName());
      nt.setId((int) id);
      return nt;
    }
    int updated = execute("UPDATE need_types SET name=? WHERE id=?", nt.getName(), nt.getId());
    if (updated == 0) throw new IllegalArgumentException("Need type not found: #" + nt.getId());
    return nt;
  }

  @Override
  public void deleteNeedType(int id) {
    try {
      int deleted = execute("DELETE FROM need_types WHERE id=?", id);
      if (deleted == 0) throw new IllegalArgumentException("Need type not found: #" + id);
    } catch (IllegalStateException e) {
      if (e.getMessage() != null && e.getMessage().contains("FOREIGN KEY")) {
        throw new IllegalStateException("Cannot delete need type #" + id + " because active beneficiaries are assigned to it.", e);
      }
      throw new IllegalStateException("Could not delete need type: " + e.getMessage(), e);
    }
  }

  private Beneficiary insert(Beneficiary b) throws java.sql.SQLException {
    long id = insertReturnKey(
      "INSERT INTO beneficiaries (name, phone, household_size, need_type_id, status, notes, impact_score) VALUES (?, ?, ?, ?, ?, ?, ?)",
      b.getName(), b.getPhone(), b.getHouseholdSize(), b.getNeedTypeId(),
      b.getStatus().name(), b.getNotes(), b.getImpactScore()
    );
    b.setId(id);
    return b;
  }
}
