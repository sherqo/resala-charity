package com.resala.core;

import com.resala.model.AuditLog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe central manager that collects audit logs for pattern execution,
 * outbound notifications, and system events.
 */
public class AuditLogManager {

    private static final AuditLogManager INSTANCE = new AuditLogManager();
    private static final int MAX_LOGS = 500;

    private final AtomicLong counter = new AtomicLong(1);
    private final ObservableList<AuditLog> logs = FXCollections.observableArrayList();

    private AuditLogManager() {}

    public static AuditLogManager getInstance() {
        return INSTANCE;
    }

    public void log(String pattern, String module, String category, String recipient, String summary, String details) {
        AuditLog entry = new AuditLog(
            counter.getAndIncrement(),
            LocalDateTime.now(),
            pattern,
            module,
            category,
            recipient != null && !recipient.isBlank() ? recipient : "System",
            summary,
            details
        );

        try {
            if (Platform.isFxApplicationThread()) {
                addEntry(entry);
            } else {
                Platform.runLater(() -> addEntry(entry));
            }
        } catch (IllegalStateException e) {
            // Headless / console test mode where JavaFX toolkit is not active
            addEntry(entry);
        }
    }

    private synchronized void addEntry(AuditLog entry) {
        logs.add(0, entry);
        if (logs.size() > MAX_LOGS) {
            logs.remove(logs.size() - 1);
        }
    }

    public ObservableList<AuditLog> getLogs() {
        return logs;
    }

    public void clear() {
        try {
            if (Platform.isFxApplicationThread()) {
                logs.clear();
            } else {
                Platform.runLater(logs::clear);
            }
        } catch (IllegalStateException e) {
            logs.clear();
        }
    }
}
