package com.resala.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton that owns the one SQLite connection used by the application.
 */
public class DatabaseManager {

  private static final String SQLITE_PREFIX = "jdbc:sqlite:";

  private static DatabaseManager instance;

  private final Connection connection;

  private DatabaseManager() {
    String url = ConfigManager.getInstance().get("db.url");
    if (url == null || url.isBlank()) {
      throw new IllegalStateException("Missing db.url in config.properties");
    }

    createParentDirectory(url);

    try {
      connection = DriverManager.getConnection(url);

      try (Statement st = connection.createStatement()) {
        st.execute("PRAGMA foreign_keys = ON"); // SQLite keeps it off by default
      }

      runScript("/schema.sql");
      if (isEmpty("users")) {
        runScript("/seed.sql");
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to open database: " + url, e);
    }
  }

  public static synchronized DatabaseManager getInstance() {
    if (instance == null) {
      instance = new DatabaseManager();
    }
    return instance;
  }

  public Connection getConnection() {
    return connection;
  }

  public void close() {
    try {
      connection.close();
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to close database", e);
    }
  }

  /** Executes every statement of a classpath SQL file (statements separated by ';'). */
  private void runScript(String resourcePath) {
    for (String sql : readResource(resourcePath).split(";")) {
      String statement = sql.replaceAll("(?m)--.*$", "").trim(); // drop -- comments
      if (statement.isEmpty()) {
        continue;
      }
      try (Statement st = connection.createStatement()) {
        st.execute(statement);
      } catch (SQLException e) {
        throw new IllegalStateException("Failed to run " + resourcePath + " -> " + statement, e);
      }
    }
  }

  /** True when the table is missing or empty, i.e. nothing to keep. */
  private boolean isEmpty(String table) {
    try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
      return rs.next() && rs.getInt(1) == 0;
    } catch (SQLException e) {
      return true; // table not created yet
    }
  }

  private String readResource(String resourcePath) {
    try (InputStream in = DatabaseManager.class.getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new IllegalStateException(resourcePath + " not found on classpath");
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + resourcePath, e);
    }
  }

  /** Creates the folder that will hold the SQLite file (e.g. data/). */
  private void createParentDirectory(String url) {
    if (!url.startsWith(SQLITE_PREFIX) || url.equals(SQLITE_PREFIX + ":memory:")) {
      return;
    }

    Path file = Path.of(url.substring(SQLITE_PREFIX.length())).toAbsolutePath();
    Path directory = file.getParent();
    if (directory == null) {
      return;
    }

    try {
      Files.createDirectories(directory);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create " + directory, e);
    }
  }
}
