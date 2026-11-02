package com.resala.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class TransactionManagerTest {

  @Test
  void commitsSuccessfulWork() throws Exception {
    try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
      createTable(conn);
      TransactionManager tx = new TransactionManager(conn);

      tx.inTransaction(() -> {
        insert(conn, "a");
        return null;
      });

      assertEquals(1, count(conn));
    }
  }

  @Test
  void rollsBackOnErrorNotOnlyOnException() throws Exception {
    try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
      createTable(conn);
      TransactionManager tx = new TransactionManager(conn);

      assertThrows(AssertionError.class, () -> tx.inTransaction(() -> {
        insert(conn, "a");
        throw new AssertionError("boom");
      }));

      assertEquals(0, count(conn));
    }
  }

  @Test
  void nestedTransactionJoinsTheOuterTransaction() throws Exception {
    try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
      createTable(conn);
      TransactionManager tx = new TransactionManager(conn);

      assertThrows(IllegalStateException.class, () -> tx.inTransaction(() -> {
        insert(conn, "outer");
        tx.inTransaction(() -> {
          insert(conn, "inner");
          return null;
        });
        throw new IllegalStateException("rollback outer");
      }));

      assertEquals(0, count(conn));
    }
  }

  private void createTable(Connection conn) throws Exception {
    try (Statement st = conn.createStatement()) {
      st.execute("CREATE TABLE t (v TEXT)");
    }
  }

  private void insert(Connection conn, String value) {
    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO t (v) VALUES (?)")) {
      ps.setString(1, value);
      ps.executeUpdate();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private int count(Connection conn) throws Exception {
    try (Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM t")) {
      return rs.getInt(1);
    }
  }
}
