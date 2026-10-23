package com.resala.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public class TransactionManager {

  private final Connection connection;

  public TransactionManager(Connection connection) {
    this.connection = connection;
  }

  public <T> T inTransaction(Supplier<T> work) {
    boolean previous;
    try {
      previous = connection.getAutoCommit();
      connection.setAutoCommit(false);
    } catch (SQLException e) {
      throw new IllegalStateException("Could not begin transaction", e);
    }
    try {
      T result = work.get();
      try { connection.commit(); } catch (SQLException e) { throw new IllegalStateException("Could not commit transaction", e); }
      return result;
    } catch (RuntimeException e) {
      try { connection.rollback(); } catch (SQLException ignored) {}
      throw e;
    } finally {
      try { connection.setAutoCommit(previous); } catch (SQLException ignored) {}
    }
  }
}