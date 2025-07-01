package eu.hindustries.timur.hwatchdog.managers;

import eu.hindustries.timur.hwatchdog.HWatchdogPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {

    private final HWatchdogPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(HWatchdogPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public List<String> getWatchedCommands() {
        return config.getStringList("watched-commands").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    public boolean isDiscordEnabled() { return config.getBoolean("discord.enabled", false); }
    public String getPublicWebhookUrl() { return config.getString("discord.public-webhook-url", ""); }
    public String getReportWebhookUrl() { return config.getString("discord.report-webhook-url", ""); }
    public String getMentionRoleId() { return config.getString("discord.mention-role-id", ""); }
    public boolean useEmbeds() { return config.getBoolean("discord.use-embeds", true); }

    public boolean isAutoDemoteEnabled() { return config.getBoolean("auto-demote.enabled", false); }
    public int getAutoDemoteThreshold() { return config.getInt("auto-demote.threshold", 3); }
    public int getAutoDemoteCooldown() { return config.getInt("auto-demote.cooldown-seconds", 60); }

    public List<String> getExcludedOps() {
        return config.getStringList("excluded-ops").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    public int getReasonExpirySeconds() { return config.getInt("reason-expiry-seconds", 120); }
    public int getReportCooldownSeconds() { return config.getInt("report-cooldown-seconds", 60); }
    public boolean includeCoordinatesInLogs() { return config.getBoolean("include-coordinates-in-logs", false); }
}