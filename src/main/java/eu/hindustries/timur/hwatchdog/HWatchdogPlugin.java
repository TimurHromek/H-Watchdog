package eu.hindustries.timur.hwatchdog;

import eu.hindustries.timur.hwatchdog.commands.ReasonCommand;
import eu.hindustries.timur.hwatchdog.commands.ReportOPCommand;
import eu.hindustries.timur.hwatchdog.listeners.CommandMonitorListener;
import eu.hindustries.timur.hwatchdog.managers.ConfigManager;
import eu.hindustries.timur.hwatchdog.managers.DiscordWebhookSender;
import eu.hindustries.timur.hwatchdog.managers.FileLogger;
import eu.hindustries.timur.hwatchdog.managers.OPSessionManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class HWatchdogPlugin extends JavaPlugin {

    private static HWatchdogPlugin instance;
    private ConfigManager configManager;
    private OPSessionManager opSessionManager;
    private DiscordWebhookSender discordWebhookSender;
    private FileLogger fileLogger;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        configManager = new ConfigManager(this);
        opSessionManager = new OPSessionManager(this);
        discordWebhookSender = new DiscordWebhookSender(this);
        fileLogger = new FileLogger(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new CommandMonitorListener(this), this);

        // Register commands
        getCommand("reason").setExecutor(new ReasonCommand(this));
        getCommand("reportop").setExecutor(new ReportOPCommand(this));

        getLogger().info("H-Watchdog has been enabled and is monitoring OP activity.");
    }

    @Override
    public void onDisable() {
        getLogger().info("H-Watchdog has been disabled.");
    }

    public static HWatchdogPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public OPSessionManager getOpSessionManager() {
        return opSessionManager;
    }

    public DiscordWebhookSender getDiscordWebhookSender() {
        return discordWebhookSender;
    }

    public FileLogger getFileLogger() {
        return fileLogger;
    }
}