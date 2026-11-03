package com.resala.model;

import java.time.LocalDateTime;

/**
 * Represents a single audit log entry tracking pattern executions,
 * outgoing notifications, and system events.
 */
public class AuditLog {

    private final long id;
    private final LocalDateTime timestamp;
    private final String pattern;     // e.g. "Observer", "Strategy", "Command"
    private final String module;      // e.g. "Events", "Volunteers", "Donations", "Payments"
    private final String category;    // "Observer Triggers", "Outgoing Notifications", "Payment Operations"
    private final String recipient;   // e.g. "user@example.com" or "Donor #1"
    private final String summary;     // Brief description
    private final String details;     // Full payload, message body, or pattern explanation

    public AuditLog(long id, LocalDateTime timestamp, String pattern, String module,
                    String category, String recipient, String summary, String details) {
        this.id = id;
        this.timestamp = timestamp;
        this.pattern = pattern;
        this.module = module;
        this.category = category;
        this.recipient = recipient;
        this.summary = summary;
        this.details = details;
    }

    public long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getPattern() { return pattern; }
    public String getModule() { return module; }
    public String getCategory() { return category; }
    public String getRecipient() { return recipient; }
    public String getSummary() { return summary; }
    public String getDetails() { return details; }
}
