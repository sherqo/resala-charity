package com.resala.model;

import java.time.LocalDateTime;

public class AuditLog {

    private final long id;
    private final LocalDateTime timestamp;
    private final String pattern;
    private final String module;
    private final String category;
    private final String recipient;
    private final String summary;
    private final String details;

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
