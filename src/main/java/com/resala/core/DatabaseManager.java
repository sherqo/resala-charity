package com.resala.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
        st.execute("PRAGMA foreign_keys = ON"); // SQLite foreign keys must be enabled for each connection.
      }

      runScript("/schema.sql");
      migrate(connection);
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

  private void runScript(String resourcePath) {
    String script = readResource(resourcePath).replaceAll("(?m)--.*$", "").trim();
    // Remove SQL comments before splitting statements.
    for (String sql : script.split(";")) {
      String statement = sql.trim();
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

  private void migrate(Connection connection) {
    try (Statement st = connection.createStatement()) {
      st.execute("""
        CREATE TABLE IF NOT EXISTS donation_types (
            id    INTEGER PRIMARY KEY AUTOINCREMENT,
            name  TEXT NOT NULL UNIQUE,
            code  TEXT NOT NULL UNIQUE CHECK (code IN ('ONLINE','CHECK','IN_KIND'))
        )
        """);
      st.execute("ALTER TABLE payments ADD COLUMN fee_minor INTEGER NOT NULL DEFAULT 0");
    } catch (SQLException ignored) {
      // The column may already exist in a new database.
    }

    try (Statement st = connection.createStatement()) {
      st.execute("ALTER TABLE event_registrations ADD COLUMN volunteer_id INTEGER REFERENCES volunteers(id)");
    } catch (SQLException ignored) {}

    try (Statement st = connection.createStatement()) {
      st.execute("ALTER TABLE event_registrations ADD COLUMN skill_id INTEGER REFERENCES skills(id)");
    } catch (SQLException ignored) {}

    try (Statement st = connection.createStatement()) {
      st.execute("ALTER TABLE events ADD COLUMN hours INTEGER NOT NULL DEFAULT 0");
    } catch (SQLException ignored) {}
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
