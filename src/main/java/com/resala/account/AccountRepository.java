package com.resala.account;

import com.resala.core.JdbcRepository;
import com.resala.model.Account;
import com.resala.model.Role;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AccountRepository extends JdbcRepository<Account> {

    private static final String SELECT_ACCOUNT =
            "SELECT id, username, password_hash, full_name, email, role, volunteer_id FROM accounts";


    public  AccountRepository(){}

    public AccountRepository(Connection conn){
        super(conn);
    }

    @Override
    public long insert(Account a) {
        String sql = """
                INSERT INTO accounts (username, password_hash, full_name, email, role, volunteer_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        long id = insertReturnKey(sql,
                a.getUsername(),
                a.getPasswordHash(),
                a.getFullName(),
                a.getEmail(),
                roleOf(a),
                a.getVolunteerId());
        a.setId(id);
        return id;
    }

    @Override
    public boolean update(Account a) {
        String sql = """
                UPDATE accounts
                   SET username = ?, password_hash = ?, full_name = ?, email = ?, role = ?, volunteer_id = ?
                 WHERE id = ?
                """;
        return execute(sql,
                a.getUsername(),
                a.getPasswordHash(),
                a.getFullName(),
                a.getEmail(),
                roleOf(a),
                a.getVolunteerId(),
                a.getId()) > 0;
    }



    @Override
    public boolean deleteById(long id) {
        return execute("DELETE FROM accounts WHERE id = ?", id) > 0;
    }

    @Override
    public Optional<Account> findById(long id) {
        return queryOne(SELECT_ACCOUNT + " WHERE id = ?", AccountRepository::mapAccount, id);
    }

    @Override
    public List<Account> findAll() {
        return query(SELECT_ACCOUNT + " ORDER BY username", AccountRepository::mapAccount);
    }

    public Optional<Account> findByUsername(String username) {
        return queryOne(SELECT_ACCOUNT + " WHERE username = ?",
                AccountRepository::mapAccount, username);
    }

    public boolean existsByUsername(String username) {
        return queryOne("SELECT 1 AS one FROM accounts WHERE username = ? LIMIT 1",
                rs -> rs.getInt("one"), username).isPresent();
    }

    public List<Account> findByRole(Role role) {
        return query(SELECT_ACCOUNT + " WHERE role = ? ORDER BY username",
                AccountRepository::mapAccount, roleOf(role));
    }

    public Optional<Account> findByVolunteerId(long volunteerId) {
        return queryOne(SELECT_ACCOUNT + " WHERE volunteer_id = ?",
                AccountRepository::mapAccount, volunteerId);
    }

    public boolean updatePasswordHash(long id, String passwordHash) {
        return execute("UPDATE accounts SET password_hash = ? WHERE id = ?", passwordHash, id) > 0;
    }

    public long count() {
        return queryOne("SELECT COUNT(*) AS total FROM accounts", rs -> rs.getLong("total")).orElse(0L);
    }

    private static String roleOf(Account a) {
        return roleOf(a.getRole());
    }

    private static String roleOf(Role role) {
        return role == null ? null : role.name();
    }

    private static Account mapAccount(ResultSet rs) throws SQLException {
        return new Account(
                lng(rs, "id"),
                str(rs, "username"),
                str(rs, "password_hash"),
                str(rs, "full_name"),
                str(rs, "email"),
                Role.valueOf(str(rs, "role")),
                nullableLong(rs, "volunteer_id"));
    }

    private static Long nullableLong(ResultSet rs, String col) throws SQLException {
        long value = rs.getLong(col);
        return rs.wasNull() ? null : value;
    }
}
