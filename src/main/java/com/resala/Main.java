package com.resala;

import com.resala.core.ConfigManager;
import com.resala.core.DatabaseManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {

    /** TEMPORARY diagnostic: written on every run so we can inspect failures without console copy-paste. */
    private static final Path REPORT = Path.of("/tmp/opencode/last-run-report.txt");

    public static void main(String[] args) {
        try {
            smokeTest();
            writeReport("RUN OK", null);
        } catch (Throwable t) {
            writeReport("RUN FAILED", t);
            System.exit(1);
        }
    }

    private static void smokeTest() throws Exception {

        // --- ConfigManager ---
        ConfigManager config = ConfigManager.getInstance();
        System.out.println("db.url        = " + config.get("db.url"));
        System.out.println("missing key   = " + config.get("no.such.key"));              // expect null
        System.out.println("default value = " + config.get("no.such.key", "fallback"));   // expect fallback

        // --- DatabaseManager ---
        DatabaseManager db = DatabaseManager.getInstance();
        System.out.println("singleton ok  = " + (db == DatabaseManager.getInstance()));   // expect true

        Connection conn = db.getConnection();
        System.out.println("isClosed      = " + conn.isClosed());                         // expect false

        // --- PRAGMA foreign_keys actually applied? ---
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA foreign_keys")) {
            rs.next();
            System.out.println("foreign_keys  = " + rs.getInt(1));                        // expect 1
        }

        // --- write / read round trip ---
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS smoke_test (id INTEGER PRIMARY KEY)");
            st.execute("INSERT INTO smoke_test DEFAULT VALUES");
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM smoke_test")) {
                rs.next();
                System.out.println("smoke rows    = " + rs.getInt(1));
            }
        }

        // --- what did schema.sql create? ---
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")) {
            while (rs.next()) {
                System.out.println("table         = " + rs.getString(1));
            }
        }

        db.close();
        System.out.println("close() OK");
    }

    private static void writeReport(String status, Throwable t) {
        StringWriter sw = new StringWriter();
        if (t != null) {
            t.printStackTrace(new PrintWriter(sw));
        }
        String report = """
                status      = %s
                time        = %s
                user.dir    = %s
                java.version = %s
                class.path  = %s

                %s
                """.formatted(
                status,
                java.time.ZonedDateTime.now(),
                System.getProperty("user.dir"),
                System.getProperty("java.version"),
                System.getProperty("java.class.path"),
                sw.toString());
        try {
            Files.createDirectories(REPORT.getParent());
            Files.writeString(REPORT, report);
        } catch (IOException e) {
            System.err.println("could not write report: " + e.getMessage());
        }
        if (t != null) {
            System.err.println(report);
        }
    }
}
