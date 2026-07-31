package com.velocityreport;

import com.velocityreport.model.Report;
import com.velocityreport.model.Report.Status;
import com.velocityreport.storage.ReportStorage;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core business logic for the report plugin.
 * <p>
 * Manages cooldowns, message formatting, staff notifications,
 * and orchestrates report creation / closure through the storage layer.
 * All user-facing messages are loaded from the LanguageManager.
 */
public class ReportManager {

    private final ProxyServer proxy;
    private final ReportStorage storage;
    private final Logger logger;
    private final LanguageManager languageManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // In-memory cooldown: reporter UUID → last-report timestamp (epoch ms)
    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    // Staff usernames (case-insensitive lookup) — used when no permission plugin is installed
    private final Set<String> staffUsers = new HashSet<>();

    // Server names that have localized aliases (e.g. "lobby" → "登录服" / "Lobby")
    private final Set<String> serverAliases = new HashSet<>();

    // Report reason templates (only id + icon; name and description come from language file)
    private List<ReportReason> reportReasons = List.of(
        new ReportReason("hacking", "⛏"),
        new ReportReason("chat", "💬"),
        new ReportReason("griefing", "💥"),
        new ReportReason("admin-abuse", "⚡"),
        new ReportReason("other", "📋")
    );

    public static final class ReportReason {
        private final String id;
        private final String icon;

        public ReportReason(String id, String icon) {
            this.id = id;
            this.icon = icon;
        }

        public String getId() { return id; }
        public String getIcon() { return icon; }
    }

    private int cooldownSeconds = 60;
    private int reportsPerPage = 10;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ReportManager(ProxyServer proxy, ReportStorage storage, Logger logger, LanguageManager languageManager) {
        this.proxy = proxy;
        this.storage = storage;
        this.logger = logger;
        this.languageManager = languageManager;
    }

    // ── Public message API (for commands) ─────────────────────

    /**
     * Get a localized message string by key.
     * Delegates to the LanguageManager.
     */
    public String getMessage(String key) {
        return languageManager.getMessage(key);
    }

    // ── Configuration ────────────────────────────────────────

    public void setCooldownSeconds(int seconds) { this.cooldownSeconds = Math.max(0, seconds); }
    public void setReportsPerPage(int n) { this.reportsPerPage = Math.max(1, n); }

    // ── Report creation ──────────────────────────────────────

    /**
     * Attempts to file a report. Returns an error Component on failure,
     * or the success Component on success (report was filed and staff notified).
     */
    public Component fileReport(Player reporter, String reportedName, String reason) {
        // ── Self-report check ──
        if (reporter.getUsername().equalsIgnoreCase(reportedName)) {
            return buildMessage("self-report");
        }

        // ── Cooldown check ──
        if (!reporter.hasPermission("velocityreport.bypasscooldown")) {
            long now = System.currentTimeMillis();
            Long last = cooldowns.get(reporter.getUniqueId());
            if (last != null) {
                long elapsed = (now - last) / 1000;
                if (elapsed < cooldownSeconds) {
                    return buildMessage("report-cooldown", "{time}", String.valueOf(cooldownSeconds - elapsed));
                }
            }
        }

        // ── Find reported player ──
        Optional<Player> targetOpt = proxy.getPlayer(reportedName);
        if (targetOpt.isEmpty()) {
            return buildMessage("player-not-found", "{player}", reportedName);
        }

        Player target = targetOpt.get();
        UUID reportedUuid = target.getUniqueId();
        String reportedServer = target.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("unknown");

        // ── Reporter server ──
        String reporterServer = reporter.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse("unknown");

        // ── Create and persist ──
        Report report = new Report(
                reporter.getUniqueId(), reporter.getUsername(),
                reportedUuid, target.getUsername(),
                reason, reporterServer, reportedServer);

        try {
            int id = storage.createReport(report);
            report.setId(id);
        } catch (SQLException e) {
            logger.error("Failed to save report to database", e);
            return buildMessage("report-internal-error");
        }

        // ── Update cooldown ──
        cooldowns.put(reporter.getUniqueId(), System.currentTimeMillis());

        // ── Notify staff online ──
        Component notify = buildMessage("staff-notify",
                "{reporter}", reporter.getUsername(),
                "{reported}", target.getUsername(),
                "{server}", resolveServerName(reporterServer),
                "{reason}", reason);
        notifyStaff(notify);

        // ── Log ──
        logger.info("Report #{} — {} reported {} on {}: {}",
                report.getId(), reporter.getUsername(), target.getUsername(), reporterServer, reason);

        // ── Confirm to reporter ──
        return buildMessage("report-sent", "{reported}", target.getUsername());
    }

    // ── Report listing ───────────────────────────────────────

    public Component listReports(int page) {
        try {
            int total = storage.getOpenReportCount();
            if (total == 0) {
                return buildMessage("no-reports");
            }

            int totalPages = Math.max(1, (int) Math.ceil((double) total / reportsPerPage));
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;

            List<Report> reports = storage.getOpenReports(page, reportsPerPage);

            Component header = buildMessage("report-list-header",
                    "{page}", String.valueOf(page),
                    "{total}", String.valueOf(totalPages));

            var builder = Component.text().append(header);
            for (Report r : reports) {
                builder.append(Component.newline());
                builder.append(buildMessage("report-entry",
                        "{id}", String.valueOf(r.getId()),
                        "{reported}", r.getReportedName(),
                        "{reporter}", r.getReporterName(),
                        "{server}", resolveServerName(r.getServerName()),
                        "{time}", dateFormat.format(new Date(r.getTimestamp()))));
            }

            return builder.build();
        } catch (SQLException e) {
            logger.error("Failed to fetch reports", e);
            return buildMessage("report-internal-error-short");
        }
    }

    // ── Report detail ────────────────────────────────────────

    public Component viewReport(int id) {
        try {
            Report r = storage.getReport(id);
            if (r == null) {
                return buildMessage("invalid-report-id", "{id}", String.valueOf(id));
            }

            String handlerInfo = "";
            if (r.getStatus() != Status.OPEN && r.getHandlerName() != null) {
                String resolution = r.getResolution() != null ? r.getResolution() : "N/A";
                String handledAt = r.getHandledAt() > 0
                        ? dateFormat.format(new Date(r.getHandledAt()))
                        : "N/A";
                handlerInfo = "<newline><gray>Handled by:</gray> <white>" + r.getHandlerName() + "</white>" +
                        "<newline><gray>Resolution:</gray> <white>" + resolution + "</white>" +
                        "<newline><gray>Handled at:</gray> <white>" + handledAt + "</white>";
            }

            String msg = languageManager.getMessage("report-detail")
                    .replace("{handler_info}", handlerInfo);

            return buildMessageRaw(msg,
                    "{id}", String.valueOf(r.getId()),
                    "{reporter}", r.getReporterName(),
                    "{reported}", r.getReportedName(),
                    "{server}", resolveServerName(r.getServerName()),
                    "{reported_server}", resolveServerName(r.getReportedServerName()),
                    "{reason}", r.getReason(),
                    "{time}", dateFormat.format(new Date(r.getTimestamp())),
                    "{status}", formatStatus(r.getStatus()));
        } catch (SQLException e) {
            logger.error("Failed to fetch report #{}", id, e);
            return buildMessage("report-internal-error-short");
        }
    }

    // ── Report closure ───────────────────────────────────────

    public Component closeReport(int id, Player handler, String resolution) {
        try {
            Report r = storage.getReport(id);
            if (r == null) {
                return buildMessage("invalid-report-id", "{id}", String.valueOf(id));
            }
            if (r.getStatus() != Status.OPEN) {
                return buildMessage("already-closed", "{id}", String.valueOf(id),
                        "{status}", formatStatus(r.getStatus()));
            }

            boolean updated = storage.closeReport(id, Status.RESOLVED,
                    handler.getUniqueId(), handler.getUsername(), resolution);
            if (updated) {
                logger.info("Report #{} resolved by {}", id, handler.getUsername());
                return buildMessage("report-closed", "{id}", String.valueOf(id),
                        "{status}", formatStatus(Status.RESOLVED));
            }
            return buildMessage("report-failed-update");
        } catch (SQLException e) {
            logger.error("Failed to close report #{}", id, e);
            return buildMessage("report-internal-error-short");
        }
    }

    // ── History GUI / Toggle ─────────────────────────────────

    /**
     * Builds a clickable report history list for staff.
     * If playerName is provided, filters reports involving that player.
     */
    public Component buildReportHistoryList(int page, String playerName) {
        try {
            int total;
            List<Report> reports;
            if (playerName != null && !playerName.isBlank()) {
                total = storage.getReportCountByPlayerName(playerName);
                reports = storage.getReportsByPlayerName(playerName, page, reportsPerPage);
            } else {
                total = storage.getTotalReportCount();
                reports = storage.getAllReports(page, reportsPerPage);
            }

            if (total == 0) {
                Component msg = playerName != null
                    ? buildMessage("report-history-player-not-found", "{player}", playerName)
                    : buildMessage("no-reports");
                return msg;
            }

            int totalPages = Math.max(1, (int) Math.ceil((double) total / reportsPerPage));
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;

            // Build title
            String titleKey = playerName != null ? "report-history-title-player" : "report-history-title-all";
            String title = languageManager.getMessage(titleKey)
                    .replace("{page}", String.valueOf(page))
                    .replace("{total}", String.valueOf(totalPages));
            if (playerName != null) {
                title = title.replace("{player}", playerName);
            }

            var builder = Component.text()
                .append(buildMessageRaw(title))
                .append(Component.newline()).append(Component.newline());

            for (Report r : reports) {
                String statusIcon = r.getStatus() == Status.OPEN
                        ? languageManager.getMessage("report-history-status-icon-open")
                        : languageManager.getMessage("report-history-status-icon-closed");

                String statusLabel = r.getStatus() == Status.OPEN
                        ? languageManager.getMessage("status-open-plain")
                        : languageManager.getMessage("status-resolved-plain");

                String line = languageManager.getMessage("report-history-entry-format")
                        .replace("{status_icon}", statusIcon)
                        .replace("{reporter}", r.getReporterName())
                        .replace("{reported}", r.getReportedName())
                        .replace("{reason}", r.getReason());

                // Clickable entry with hover details
                String safeReason = r.getReason().replace("'", "''").replace("<", "＜").replace(">", "＞");
                String hoverText = buildHoverText(r, statusLabel, safeReason);

                String clickable = "<click:run_command:'/reportcd view " + r.getId() + "'>"
                    + "<hover:show_text:'" + hoverText + "'>"
                    + translateLegacyColors(line)
                    + "</hover></click>";

                builder.append(buildMessageRaw(" " + clickable))
                       .append(Component.newline());
            }

            // Navigation buttons
            builder.append(Component.newline());
            if (page > 1) {
                String prevCmd = playerName != null
                    ? "/reportcd list " + (page - 1) + " " + playerName
                    : "/reportcd list " + (page - 1);
                builder.append(buildMessageRaw(languageManager.getMessage("report-history-nav-prev")
                    .replace("{command}", prevCmd)
                    .replace("{hover}", languageManager.getMessage("report-history-hover-prev"))));
                builder.append(buildMessageRaw(" "));
            }
            builder.append(buildMessageRaw(languageManager.getMessage("report-history-nav-page")
                    .replace("{page}", String.valueOf(page))
                    .replace("{total}", String.valueOf(totalPages))));
            builder.append(buildMessageRaw(" "));
            if (page < totalPages) {
                String nextCmd = playerName != null
                    ? "/reportcd list " + (page + 1) + " " + playerName
                    : "/reportcd list " + (page + 1);
                builder.append(buildMessageRaw(languageManager.getMessage("report-history-nav-next")
                    .replace("{command}", nextCmd)
                    .replace("{hover}", languageManager.getMessage("report-history-hover-next"))));
            }

            return builder.build();
        } catch (SQLException e) {
            logger.error("Failed to load report history", e);
            return buildMessage("report-internal-error-short");
        }
    }

    /**
     * Builds the hover text for a report history entry using the language template.
     */
    private String buildHoverText(Report r, String statusLabel, String safeReason) {
        String template = languageManager.getMessage("report-history-hover-template");
        return template
                .replace("{reporter}", r.getReporterName())
                .replace("{reported}", r.getReportedName())
                .replace("{reason}", safeReason)
                .replace("{status}", statusLabel);
    }

    /**
     * Builds a detailed view of a report with action buttons.
     */
    public Component buildReportDetailMenu(int id) {
        try {
            Report r = storage.getReport(id);
            if (r == null) {
                return buildMessage("invalid-report-id", "{id}", String.valueOf(id));
            }

            StringBuilder detail = new StringBuilder();
            detail.append(translateLegacyColors(
                    languageManager.getMessage("report-detail-title")
                            .replace("{id}", String.valueOf(id))));
            detail.append("\n").append(translateLegacyColors(
                    languageManager.getMessage("report-detail-reporter")
                            .replace("{reporter}", r.getReporterName())));
            detail.append("\n").append(translateLegacyColors(
                    languageManager.getMessage("report-detail-reported")
                            .replace("{reported}", r.getReportedName())));
            detail.append("\n").append(translateLegacyColors(
                    languageManager.getMessage("report-detail-reason")
                            .replace("{reason}", r.getReason())));
            detail.append("\n").append(translateLegacyColors(
                    languageManager.getMessage("report-detail-reporter-server")
                            .replace("{server}", resolveServerName(r.getServerName()))));
            detail.append("\n").append(translateLegacyColors(
                    languageManager.getMessage("report-detail-reported-server")
                            .replace("{server}", resolveServerName(r.getReportedServerName()))));
            detail.append("\n").append(translateLegacyColors(
                    languageManager.getMessage("report-detail-time")
                            .replace("{time}", dateFormat.format(new Date(r.getTimestamp())))));
            detail.append("\n").append(translateLegacyColors(
                    languageManager.getMessage("report-detail-status")
                            .replace("{status}", languageManager.getMessage(
                                    r.getStatus() == Status.OPEN ? "status-open" : "status-resolved"))));

            String handlerInfo = "";
            if (r.getStatus() != Status.OPEN && r.getHandlerName() != null) {
                String resolution = r.getResolution() != null ? r.getResolution() : "N/A";
                String handledAt = r.getHandledAt() > 0
                        ? dateFormat.format(new Date(r.getHandledAt()))
                        : "N/A";
                handlerInfo = "\n" + translateLegacyColors(
                        languageManager.getMessage("report-detail-handler")
                                .replace("{handler}", r.getHandlerName()));
                handlerInfo += "\n" + translateLegacyColors(
                        languageManager.getMessage("report-detail-resolution-label")
                                .replace("{resolution}", resolution));
                handlerInfo += "\n" + translateLegacyColors(
                        languageManager.getMessage("report-detail-handled-at")
                                .replace("{handled_at}", handledAt));
            }
            detail.append(handlerInfo);

            var builder = Component.text()
                .append(buildMessageRaw(detail.toString()))
                .append(Component.newline()).append(Component.newline());

            // Action buttons
            if (r.getStatus() == Status.OPEN) {
                builder.append(buildMessageRaw(
                    languageManager.getMessage("report-detail-btn-resolve")
                        .replace("{id}", String.valueOf(id))
                        .replace("{hover}", languageManager.getMessage("report-detail-hover-resolve"))));
            } else {
                builder.append(buildMessageRaw(
                    languageManager.getMessage("report-detail-btn-reopen")
                        .replace("{id}", String.valueOf(id))
                        .replace("{hover}", languageManager.getMessage("report-detail-hover-reopen"))));
            }

            // Back to list
            builder.append(buildMessageRaw(" "));
            builder.append(buildMessageRaw(
                languageManager.getMessage("report-detail-btn-back")
                    .replace("{hover}", languageManager.getMessage("report-detail-hover-back"))));

            return builder.build();
        } catch (SQLException e) {
            logger.error("Failed to load report #{}", id, e);
            return buildMessage("report-internal-error-short");
        }
    }

    /**
     * Toggles a report between OPEN and RESOLVED.
     * When resolving, notifies the reporter if they are online.
     */
    public Component toggleReportStatus(int id, Player handler, boolean resolve) {
        try {
            Report r = storage.getReport(id);
            if (r == null) {
                return buildMessage("invalid-report-id", "{id}", String.valueOf(id));
            }

            if (resolve) {
                if (r.getStatus() != Status.OPEN) {
                    return buildMessage("already-closed", "{id}", String.valueOf(id),
                            "{status}", formatStatus(r.getStatus()));
                }
                String defaultReason = languageManager.getMessage("report-resolve-default-reason");
                storage.closeReport(id, Status.RESOLVED, handler.getUniqueId(), handler.getUsername(), defaultReason);
                logger.info("Report #{} marked as resolved by {}", id, handler.getUsername());

                // Notify the reporter if online
                String idStr = String.valueOf(id);
                proxy.getPlayer(r.getReporterUuid()).ifPresent(reporter ->
                    reporter.sendMessage(buildMessage("report-notify-resolved", "{id}", idStr))
                );

                return buildMessage("report-toggle-resolved", "{id}", idStr);
            } else {
                if (r.getStatus() == Status.OPEN) {
                    return buildMessage("report-toggle-already-open", "{id}", String.valueOf(id));
                }
                storage.reopenReport(id);
                logger.info("Report #{} reopened by {}", id, handler.getUsername());
                return buildMessage("report-toggle-reopened", "{id}", String.valueOf(id));
            }
        } catch (SQLException e) {
            logger.error("Failed to toggle report #{}", id, e);
            return buildMessage("report-internal-error-short");
        }
    }

    // ── Staff notification ───────────────────────────────────

    /** Sends a notification about open report count to a staff member who just joined. */
    public void notifyStaffOnJoin(Player player) {
        if (!hasNotifyPermission(player)) return;
        try {
            int count = storage.getOpenReportCount();
            if (count > 0) {
                player.sendMessage(buildMessage("staff-join-notify", "{count}", String.valueOf(count)));
            }
        } catch (SQLException e) {
            logger.warn("Failed to count reports for join notification", e);
        }
    }

    /** Broadcasts a notification to all online staff. */
    public void notifyStaff(Component message) {
        // message already contains the prefix via msgStaffNotify
        for (Player player : proxy.getAllPlayers()) {
            if (hasNotifyPermission(player)) {
                player.sendMessage(message);
            }
        }
    }

    /** Returns true if the player has either notify or staff permission. */
    private boolean hasNotifyPermission(Player player) {
        return player.hasPermission("velocityreport.notify")
            || player.hasPermission("velocityreport.staff")
            || staffUsers.contains(player.getUsername().toLowerCase());
    }

    /** Sets the list of staff usernames (auto-lowered for case-insensitive matching). */
    public void setStaffUsers(List<String> usernames) {
        staffUsers.clear();
        if (usernames != null) {
            for (String name : usernames) {
                if (name != null && !name.isBlank()) {
                    staffUsers.add(name.trim().toLowerCase());
                }
            }
        }
    }

    /** Sets the set of server names that have localized aliases. */
    public void setServerAliases(Collection<String> aliases) {
        serverAliases.clear();
        if (aliases != null) {
            serverAliases.addAll(aliases);
        }
    }

    /** Sets the report reason templates from config. */
    public void setReportReasons(List<ReportReason> reasons) {
        if (reasons != null && !reasons.isEmpty()) {
            this.reportReasons = reasons;
        }
    }

    public List<ReportReason> getReportReasons() {
        return reportReasons;
    }

    /**
     * Returns the localized display name for a reason ID.
     * Falls back to the ID itself if no translation is found.
     */
    public String getReasonDisplayName(String id) {
        String key = "reason-" + id;
        if (languageManager.hasMessage(key)) {
            return languageManager.getMessage(key);
        }
        return id;
    }

    /**
     * Builds a clickable chat menu for selecting report reasons.
     * Each reason is a clickable button that runs /report <target> <reason name>.
     */
    public Component buildReasonMenu(String targetName) {
        String title = languageManager.getMessage("report-reason-menu-title")
                .replace("{target}", targetName);
        var builder = Component.text()
            .append(buildMessageRaw(title))
            .append(Component.newline()).append(Component.newline());

        for (int i = 0; i < reportReasons.size(); i++) {
            ReportReason r = reportReasons.get(i);
            String displayName = translateLegacyColors(
                    languageManager.getMessage("reason-" + r.getId()));
            String description = languageManager.getMessage("reason-" + r.getId() + "-desc");

            String hover;
            if ("other".equals(r.getId())) {
                hover = languageManager.getMessage("report-reason-menu-hover-other")
                        .replace("{description}", description);
            } else {
                hover = languageManager.getMessage("report-reason-menu-hover")
                        .replace("{name}", displayName)
                        .replace("{description}", description);
            }

            String command = "/report " + targetName + " " + r.getId();

            String numberFormat = languageManager.getMessage("report-reason-menu-number-format")
                    .replace("{number}", String.valueOf(i + 1))
                    .replace("{icon}", r.getIcon())
                    .replace("{name}", displayName);

            String clickable = "<click:run_command:'" + command + "'>"
                + "<hover:show_text:'" + hover + "'>"
                + numberFormat + "</hover></click>";

            builder.append(buildMessageRaw("  " + clickable))
                   .append(Component.newline());
        }

        builder.append(Component.newline())
               .append(buildMessageRaw(
                   languageManager.getMessage("report-reason-menu-footer")
                       .replace("{target}", targetName)));

        return builder.build();
    }

    /** Resolves a raw server name to its localized display alias, or returns the raw name if no alias is configured. */
    private String resolveServerName(String raw) {
        if (raw == null) return "N/A";
        if (serverAliases.contains(raw)) {
            String alias = languageManager.getMessage("server-alias-" + raw);
            if (alias != null && !alias.startsWith("<red>")) {
                return alias;
            }
        }
        return raw;
    }

    // ── Message building ─────────────────────────────────────

    /**
     * Builds a Component from a language file message key and optional placeholder replacements.
     * <p>
     * Usage: {@code buildMessage("report-sent", "{reported}", "Steve")}
     */
    private Component buildMessage(String key, String... replacements) {
        String template = languageManager.getMessage(key);
        return buildMessageRaw(template, replacements);
    }

    /**
     * Builds a Component directly from a raw template string with placeholder replacements.
     * <p>
     * This does NOT go through the language file — use for dynamic templates
     * that are already resolved (e.g. the detail template with {handler_info}).
     */
    private Component buildMessageRaw(String template, String... replacements) {
        String resolved = template;
        for (int i = 0; i < replacements.length; i += 2) {
            resolved = resolved.replace(replacements[i], replacements[i + 1]);
        }
        // Support & color codes (e.g. &e → <yellow>, &c → <red>, &l → <bold>)
        resolved = translateLegacyColors(resolved);
        return miniMessage.deserialize(resolved);
    }

    /**
     * Converts legacy & color codes to MiniMessage tags.
     * Supports: &0-&f (colors), &k-&o (formats), &r (reset).
     */
    private static String translateLegacyColors(String input) {
        return input
            .replace("&0", "<black>")
            .replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>")
            .replace("&5", "<dark_purple>")
            .replace("&6", "<gold>")
            .replace("&7", "<gray>")
            .replace("&8", "<dark_gray>")
            .replace("&9", "<blue>")
            .replace("&a", "<green>")
            .replace("&b", "<aqua>")
            .replace("&c", "<red>")
            .replace("&d", "<light_purple>")
            .replace("&e", "<yellow>")
            .replace("&f", "<white>")
            .replace("&k", "<obfuscated>")
            .replace("&l", "<bold>")
            .replace("&m", "<strikethrough>")
            .replace("&n", "<underlined>")
            .replace("&o", "<italic>")
            .replace("&r", "<reset>");
    }

    /** Formats a status enum into a colored string for display. */
    private String formatStatus(Status status) {
        return switch (status) {
            case OPEN -> languageManager.getMessage("status-open");
            case RESOLVED -> languageManager.getMessage("status-resolved");
            case DISMISSED -> languageManager.getMessage("status-dismissed");
        };
    }
}
