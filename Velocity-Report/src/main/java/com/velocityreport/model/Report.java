package com.velocityreport.model;

import java.util.UUID;

/**
 * Represents a player report filed on the proxy.
 */
public class Report {

    public enum Status {
        OPEN,
        RESOLVED,
        DISMISSED
    }

    private int id;
    private final UUID reporterUuid;
    private final String reporterName;
    private final UUID reportedUuid;
    private final String reportedName;
    private final String reason;
    private final String serverName;
    private final String reportedServerName;
    private final long timestamp;
    private Status status;
    private UUID handlerUuid;
    private String handlerName;
    private String resolution;
    private long handledAt;

    /**
     * Constructor for creating a brand-new report (before DB insertion).
     */
    public Report(UUID reporterUuid, String reporterName,
                  UUID reportedUuid, String reportedName,
                  String reason, String serverName, String reportedServerName) {
        this.reporterUuid = reporterUuid;
        this.reporterName = reporterName;
        this.reportedUuid = reportedUuid;
        this.reportedName = reportedName;
        this.reason = reason;
        this.serverName = serverName;
        this.reportedServerName = reportedServerName;
        this.timestamp = System.currentTimeMillis();
        this.status = Status.OPEN;
    }

    /**
     * Full constructor used when loading a report from the database.
     */
    public Report(int id,
                  UUID reporterUuid, String reporterName,
                  UUID reportedUuid, String reportedName,
                  String reason, String serverName, String reportedServerName,
                  long timestamp, Status status,
                  UUID handlerUuid, String handlerName,
                  String resolution, long handledAt) {
        this.id = id;
        this.reporterUuid = reporterUuid;
        this.reporterName = reporterName;
        this.reportedUuid = reportedUuid;
        this.reportedName = reportedName;
        this.reason = reason;
        this.serverName = serverName;
        this.reportedServerName = reportedServerName;
        this.timestamp = timestamp;
        this.status = status;
        this.handlerUuid = handlerUuid;
        this.handlerName = handlerName;
        this.resolution = resolution;
        this.handledAt = handledAt;
    }

    // ── Getters ──────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public UUID getReporterUuid() { return reporterUuid; }
    public String getReporterName() { return reporterName; }

    public UUID getReportedUuid() { return reportedUuid; }
    public String getReportedName() { return reportedName; }

    public String getReason() { return reason; }

    /** The server the reporter was on when they filed the report. */
    public String getServerName() { return serverName; }

    /** The server the reported player was on (may be null if offline). */
    public String getReportedServerName() { return reportedServerName; }

    public long getTimestamp() { return timestamp; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public UUID getHandlerUuid() { return handlerUuid; }
    public void setHandlerUuid(UUID uuid) { this.handlerUuid = uuid; }

    public String getHandlerName() { return handlerName; }
    public void setHandlerName(String name) { this.handlerName = name; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public long getHandledAt() { return handledAt; }
    public void setHandledAt(long handledAt) { this.handledAt = handledAt; }
}
