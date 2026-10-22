package com.resala.core;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class JdbcRepository<T> implements CrudRepository<T> {
    protected final Connection conn;

    protected JdbcRepository(Connection c) {
        if (c == null) throw new IllegalArgumentException("Connection is null");
        conn = c;
    }

    protected  JdbcRepository() {
        this(DatabaseManager.getInstance().getConnection());
    }

    protected  int execute(String sql, Object... params){
        try (PreparedStatement ps = conn.prepareStatement(sql)){
        bind(ps, params);
        return  ps.executeUpdate();
        } catch (SQLException e) {
            throw dbError(sql, e);
        }
    }
    protected <R> List<R> query(String sql, RowMapper<R> mapper, Object... params){
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()){
                List<R> out = new ArrayList<>();
                while (rs.next()){
                    out.add(mapper.map(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw dbError(sql, e);
        }
    }
    protected <R> Optional<R> queryOne(String sql, RowMapper<R> mapper, Object... params) {
        List<R> rows = query(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
    protected long insertReturnKey(String sql, Object... params){
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()){
                if (keys.next()) return  keys.getLong(1);
                throw new IllegalStateException("No generated keys returned for " + sql);
            }
        }catch (SQLException e){
            throw dbError(sql, e);
        }
    }
    private void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) ps.setNull(i + 1, java.sql.Types.NULL);
            else                     ps.setObject(i + 1, params[i]);
        }
    }
    @Contract("_, _ -> new")
    private @NotNull RuntimeException dbError(String sql, SQLException e) {
        return new IllegalStateException("SQL failed: " + sql + " -> " + e.getMessage(), e);
    }

    protected static String str(ResultSet rs, String col) throws SQLException { return rs.getString(col); }
    protected static int    num(ResultSet rs, String col) throws SQLException { return rs.getInt(col);    }
    protected static long   lng(ResultSet rs, String col) throws SQLException { return rs.getLong(col);   }
    protected static double dbl(ResultSet rs, String col) throws SQLException { return rs.getDouble(col); }

    @FunctionalInterface
    protected interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }

}
