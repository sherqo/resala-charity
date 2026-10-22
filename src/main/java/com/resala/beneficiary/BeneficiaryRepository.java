package com.resala.beneficiary;

import com.resala.core.JdbcRepository;
import com.resala.model.Beneficiary;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class BeneficiaryRepository extends JdbcRepository<Beneficiary> {

    private static final String COLUMNS =
            "id, name, phone, household_size, need_type_id, status, notes";


    public BeneficiaryRepository() {
        super();
    }

    public BeneficiaryRepository(Connection conn) {
        super(conn);
    }

    @Override
    public long insert(Beneficiary b) {
        String sql = """
                INSERT INTO beneficiaries (name, phone, household_size, need_type_id, status, notes)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        long id = insertReturnKey(sql,
                b.getName(),
                b.getPhone(),
                b.getHouseholdSize(),
                b.getNeedTypeId(),
                statusOf(b),
                b.getNotes());

        b.setId(id);
        return id;
    }

    @Override
    public boolean update(Beneficiary b) {
        String sql = """
                UPDATE beneficiaries
                   SET name = ?, phone = ?, household_size = ?, need_type_id = ?, status = ?, notes = ?
                 WHERE id = ?
                """;

        return execute(sql,
                b.getName(),
                b.getPhone(),
                b.getHouseholdSize(),
                b.getNeedTypeId(),
                statusOf(b),
                b.getNotes(),
                b.getId()) > 0;
    }

    @Override
    public boolean deleteById(long id) {
        return execute("DELETE FROM beneficiaries WHERE id = ?", id) > 0;
    }

    @Override
    public Optional<Beneficiary> findById(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM beneficiaries WHERE id = ?",
                BeneficiaryRepository::mapRow, id);
    }

    @Override
    public List<Beneficiary> findAll() {
        return query("SELECT " + COLUMNS + " FROM beneficiaries ORDER BY name",
                BeneficiaryRepository::mapRow);
    }


    public List<Beneficiary> findByNeedType(long needTypeId) {
        return query("SELECT " + COLUMNS + " FROM beneficiaries WHERE need_type_id = ? ORDER BY name",
                BeneficiaryRepository::mapRow, needTypeId);
    }

    public List<Beneficiary> findByStatus(Beneficiary.Status status) {
        return query("SELECT " + COLUMNS + " FROM beneficiaries WHERE status = ? ORDER BY name",
                BeneficiaryRepository::mapRow, statusOf(status));
    }

    public List<Beneficiary> searchByName(String fragment) {
        return query("SELECT " + COLUMNS + " FROM beneficiaries WHERE name LIKE ? ORDER BY name",
                BeneficiaryRepository::mapRow, "%" + fragment + "%");
    }

    public long count() {
        Optional<Long> total = queryOne("SELECT COUNT(*) AS c FROM beneficiaries", rs -> rs.getLong("c"));
        return total.orElse(0L);
    }

    private static String statusOf(Beneficiary b) {
        return statusOf(b.getStatus());
    }

    private static String statusOf(Beneficiary.Status status) {
        return status == null ? Beneficiary.Status.ACTIVE.name() : status.name();
    }

    private static Beneficiary mapRow(ResultSet rs) throws SQLException {
        return new Beneficiary(
                num(rs, "id"),
                str(rs, "name"),
                str(rs, "phone"),
                num(rs, "household_size"),
                num(rs, "need_type_id"),
                Beneficiary.Status.valueOf(str(rs, "status")),
                str(rs, "notes"));
    }
}
