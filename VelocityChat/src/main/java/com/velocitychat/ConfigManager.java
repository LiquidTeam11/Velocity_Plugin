package com.velocitychat;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages plugin configuration and internationalization (i18n) messages.
 * <p>
 * Loads config.yml and language files (lang/*.yml) from the plugin's data directory.
 * Supports runtime reload and per-key overrides via config.yml's {@code messages:} section.
 */
public class ConfigManager {

    private final Logger logger;
    private final Path dataDirectory;
    private String currentLanguage = "zh_CN";

    // Raw config values
    private Map<String, Object> config;
    private final Map<String, String> messages = new LinkedHashMap<>();
    private final Set<String> serverAliases = new HashSet<>();

    public ConfigManager(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    /**
     * Load config.yml from the data directory.
     * If it doesn't exist, copy the built-in default.
     *
     * @return true if config was loaded successfully
     */
    @SuppressWarnings("unchecked")
    public boolean loadConfig() {
        Path configFile = dataDirectory.resolve("config.yml");

        // Write default config if not present
        if (!Files.exists(configFile)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.createDirectories(dataDirectory);
                    Files.copy(in, configFile);
                    logger.info("Generated default config.yml");
                }
            } catch (IOException e) {
                logger.warn("Could not write default config.yml", e);
            }
        }

        // Load config
        if (Files.exists(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                Yaml yaml = new Yaml();
                config = yaml.load(in);
                logger.info("Configuration loaded from config.yml");
            } catch (Exception e) {
                logger.warn("Failed to parse config.yml", e);
                config = new LinkedHashMap<>();
            }
        } else {
            config = new LinkedHashMap<>();
        }

        // Read language setting
        if (config.containsKey("language")) {
            currentLanguage = config.get("language").toString();
        }

        // Read server aliases list
        serverAliases.clear();
        if (config.get("server-aliases") instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) serverAliases.add(o.toString().toLowerCase());
            }
        }

        // Load language file
        loadLanguage(currentLanguage);

        // Apply message overrides from config.yml
        if (config.get("messages") instanceof Map<?, ?> msgs) {
            for (var entry : msgs.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof String value) {
                    messages.put(key, value);
                }
            }
        }

        return true;
    }

    /**
     * Load the specified language file.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>Plugin data directory: {@code lang/&lt;language&gt;.yml} (user-customizable)</li>
     *   <li>Plugin jar resources: {@code lang/&lt;language&gt;.yml} (built-in)</li>
     * </ol>
     *
     * @param language Language code, e.g. "zh_CN", "en_US"
     */
    private void loadLanguage(String language) {
        this.currentLanguage = (language != null && !language.isBlank()) ? language : "zh_CN";

        Path langDir = dataDirectory.resolve("lang");
        try {
            Files.createDirectories(langDir);
        } catch (IOException e) {
            logger.warn("Failed to create lang/ directory", e);
        }

        // If language file doesn't exist in data directory, copy from resources
        Path langFile = langDir.resolve(this.currentLanguage + ".yml");
        if (!Files.exists(langFile)) {
            String resourcePath = "lang/" + this.currentLanguage + ".yml";
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (in != null) {
                    Files.copy(in, langFile);
                    logger.info("Generated language file: lang/{}.yml", this.currentLanguage);
                }
            } catch (Exception e) {
                logger.warn("Failed to copy built-in lang/{}.yml to data directory", this.currentLanguage, e);
            }
        }

        // Load from data directory first
        boolean loaded = false;
        if (Files.exists(langFile)) {
            try (InputStream in = Files.newInputStream(langFile)) {
                loaded = loadFromStream(in);
                if (loaded) {
                    logger.info("Loaded language file: lang/{}.yml", this.currentLanguage);
                }
            } catch (Exception e) {
                logger.warn("Failed to load lang/{}.yml from data directory", this.currentLanguage, e);
            }
        }

        // Fall back to built-in resources
        if (!loaded) {
            String resourcePath = "lang/" + this.currentLanguage + ".yml";
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (in != null && loadFromStream(in)) {
                    logger.info("Loaded built-in language: {}", resourcePath);
                } else {
                    logger.error("Language file '{}' not found in plugin resources", resourcePath);
                }
            } catch (Exception e) {
                logger.error("Failed to load language file '{}' from resources", resourcePath, e);
            }
        }
    }

    /**
     * Reload config and language from disk.
     */
    public void reload() {
        messages.clear();
        loadConfig();
        logger.info("Configuration and language reloaded successfully");
    }

    // ── Message API ──────────────────────────────────────────

    /**
     * Get a localized message by key, with placeholders replaced.
     *
     * @param key        Message identifier (e.g. "qu_an.chat.message.connected")
     * @param placeholders Values to replace {0}, {1}, {2}, ... in the message
     * @return The formatted message string, or a visible error placeholder if missing
     */
    public String getMessage(String key, String... placeholders) {
        String msg = messages.get(key);
        if (msg == null) {
            logger.warn("Missing message key '{}' in language '{}'", key, currentLanguage);
            return "§cMissing message: " + key;
        }
        for (int i = 0; i < placeholders.length; i++) {
            if (placeholders[i] != null) {
                msg = msg.replace("{" + i + "}", placeholders[i]);
            }
        }
        return ColorUtils.translate(msg);
    }

    /**
     * Get the display name for a server, using its alias if defined.
     *
     * @param serverId The raw server ID (e.g. "lobby", "survival")
     * @return The display name with color codes translated
     */
    public String getServerDisplayName(String serverId) {
        if (serverId == null) return "§7unknown";

        // Check if this server has an alias in the language file
        String aliasKey = "qu_an.chat.server.name." + serverId.toLowerCase();
        if (messages.containsKey(aliasKey)) {
            return ColorUtils.translate(messages.get(aliasKey));
        }

        // Return raw server ID with a default color
        return "§7" + serverId;
    }

    /**
     * Get the proxy display name.
     */
    public String getProxyName() {
        return getMessage("qu_an.chat.proxy.name");
    }

    /**
     * Get the default chat format.
     */
    public String getChatFormat() {
        return messages.getOrDefault("qu_an.chat.message.chat.default",
                "§8[§r{0}§8][§r{1}§8]§r<{2}§r> {3}");
    }

    /**
     * Get the broadcast message format.
     */
    public String getBroadcastFormat() {
        return messages.getOrDefault("qu_an.chat.message.broadcast",
                "§6[Broadcast] §r{0}§f: {1}");
    }

    /**
     * Get broadcast command aliases from config.
     */
    @SuppressWarnings("unchecked")
    public List<String> getBroadcastAliases() {
        if (config != null && config.get("broadcast-aliases") instanceof List<?> list) {
            List<String> aliases = new ArrayList<>();
            for (Object o : list) {
                if (o != null) aliases.add(o.toString());
            }
            return aliases;
        }
        return Arrays.asList("br", "broadcast");
    }

    /**
     * Check if a server is in the server-aliases list.
     */
    public boolean hasServerAlias(String serverId) {
        return serverId != null && serverAliases.contains(serverId.toLowerCase());
    }

    /**
     * Get the notify mode for join/switch/leave messages.
     *
     * @return "all" (everyone), "admin" (admins only), or "none" (disabled)
     */
    public String getNotifyMode() {
        if (config != null && config.containsKey("notify-mode")) {
            String mode = config.get("notify-mode").toString().toLowerCase();
            if (mode.equals("all") || mode.equals("admin") || mode.equals("none")) {
                return mode;
            }
        }
        return "all";
    }

    /**
     * Get the broadcast cooldown in seconds.
     * 0 means no cooldown.
     */
    public int getBroadcastCooldown() {
        if (config != null && config.containsKey("broadcast-cooldown")) {
            Object val = config.get("broadcast-cooldown");
            if (val instanceof Number n) return n.intValue();
        }
        return 0;
    }

    /**
     * Check if broadcast cooldown bypass is enabled for velocitychat.admin.
     */
    public boolean isCooldownBypassEnabled() {
        if (config != null && config.containsKey("broadcast-cooldown-bypass")) {
            Object val = config.get("broadcast-cooldown-bypass");
            if (val instanceof Boolean b) return b;
            if (val instanceof String s) return s.equalsIgnoreCase("true");
        }
        return false;
    }

    // ── Internal Helpers ─────────────────────────────────────

    @SuppressWarnings("unchecked")
    private boolean loadFromStream(InputStream in) {
        Yaml yaml = new Yaml();
        Object loaded = yaml.load(in);
        if (loaded instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof String value) {
                    messages.put(key, value);
                } else if (entry.getKey() instanceof String key && entry.getValue() != null) {
                    messages.put(key, entry.getValue().toString());
                }
            }
            return !messages.isEmpty();
        }
        return false;
    }
}
