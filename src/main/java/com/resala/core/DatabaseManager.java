package com.resala.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() throws IOException {
        String url = ConfigManager.getInstance().get("db.url");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Missing db.url");
        }
        createParrentDirectory(url);

        try {
            connection = openConnection(url);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");   // SQLite default is OFF!
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            initSchema();
            seedIfEmpty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open database: " + url, e);
        }
    }
    public static synchronized DatabaseManager getInstance() throws IOException {
        if(instance == null){
            instance = new DatabaseManager();
        }

        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Opens the database. If the JDBC driver is not on the run classpath
     * (e.g. an IDE run configuration that didn't pick up the Maven libraries),
     * loads sqlite-jdbc directly from the local Maven repository instead of failing.
     */
    private Connection openConnection(String url) throws SQLException {
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException driverMissing) {
            Connection retry = connectFromMavenRepo(url);
            if (retry != null) {
                System.err.println("[DatabaseManager] sqlite-jdbc was not on the classpath; "
                        + "loaded it from ~/.m2 instead. Re-import the project as Maven to fix properly.");
                return retry;
            }
            throw driverMissing;
        }
    }

    /** Best-effort: build a classloader from jars already in ~/.m2 and connect without DriverManager. */
    private Connection connectFromMavenRepo(String url) {
        try {
            Path repo = Path.of(System.getProperty("user.home"), ".m2", "repository");
            List<URL> jars = new ArrayList<>();
            collectJars(repo.resolve("org/xerial/sqlite-jdbc"), jars);   // the driver
            collectJars(repo.resolve("org/slf4j"), jars);                // slf4j-api + slf4j-nop the driver needs
            if (jars.isEmpty()) {
                return null;
            }
            URLClassLoader loader = new URLClassLoader(jars.toArray(new URL[0]), getClass().getClassLoader());
            Driver driver = (Driver) Class.forName("org.sqlite.JDBC", true, loader)
                    .getDeclaredConstructor()
                    .newInstance();
            Connection conn = driver.connect(url, new Properties());
            if (conn == null) {
                return null;
            }
            return conn;
        } catch (Exception e) {
            return null;   // cannot help — the original error is more useful
        }
    }

    /** Collects *.jar files under a Maven artifact directory (skips -sources/-javadoc). */
    private void collectJars(Path artifactDir, List<URL> out) throws IOException {
        if (!Files.isDirectory(artifactDir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(artifactDir)) {
            for (Path p : walk.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".jar"))
                    .filter(f -> !f.getFileName().toString().contains("sources"))
                    .filter(f -> !f.getFileName().toString().contains("javadoc"))
                    .toList()) {
                out.add(p.toUri().toURL());
            }
        }
    }

    private void initSchema(){
        runScript("/schema.sql");
    }
    private void seedIfEmpty(){
        if(!tableExists("users")){
            return;
        }
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users");
        ){
            if(rs.next() && rs.getInt(1) == 0){
                runScript("/seed.sql");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void runScript(String resourcePath){
        String script = readResource(resourcePath);
        for (String sql : splitStatements(script)){
            if (sql.isBlank()) {
                continue;
            }
            try (Statement st = connection.createStatement()){
                st.execute(sql);
            } catch (SQLException e) {
                throw new RuntimeException("Failed executing " + resourcePath + ": " + sql, e);
            }
        }
    }

    private String[] splitStatements(String script) {
        StringBuilder cleaned = new StringBuilder();
        for (String line: script.split("\n")) {
            int comment = line.indexOf("--");
            cleaned.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');

        }
        return cleaned.toString().split(";");
    }

    private String readResource(String resourcePath){
        try(InputStream in = DatabaseManager.class.getResourceAsStream(resourcePath)) {
            if (in == null){
                throw new IllegalStateException(resourcePath + " not found");
            }
            return new String(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read", e);
        }
    }
    private boolean isSeeded(){
        return false;
    }
    private boolean tableExists(String tableName){
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM  sqlite_master WHERE type = 'table' AND name = ?"
        )
        ){
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()){
                return rs.next();
            }
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close(){
        try {
            if(!connection.isClosed()){
                connection.close();
            }
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException("Failed to close db", e));
        }
    }
    private void createParrentDirectory(String url) throws IOException {
        String prefix = "jdbc:sqlite:";
        if(!url.startsWith(prefix) || url.equals(prefix +":memory:")){
            return;
        }
        Path dbFile = Path.of(url.substring(prefix.length()));
        Path dir = dbFile.toAbsolutePath().getParent();
        if(dir != null){
           try{
               Files.createDirectories(dir);

           } catch (IOException e) {
                throw new UncheckedIOException("Failed to create directory", e);
            }
        }
    }
}
