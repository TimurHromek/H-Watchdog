package eu.hindustries.timur.hwatchdog.listeners;

import eu.hindustries.timur.hwatchdog.HWatchdogPlugin;
import eu.hindustries.timur.hwatchdog.managers.ConfigManager;
import eu.hindustries.timur.hwatchdog.managers.DiscordWebhookSender;
import eu.hindustries.timur.hwatchdog.managers.FileLogger;
import eu.hindustries.timur.hwatchdog.managers.OPSessionManager;
import eu.hindustries.timur.hwatchdog.model.OPSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CommandMonitorListener implements Listener {
    // ... (Code is identical, only imports and package changed)
    private final HWatchdogPlugin plugin;
    private final ConfigManager config;
    private final OPSessionManager sessionManager;
    private final DiscordWebhookSender discordSender;
    private final FileLogger fileLogger;

    public CommandMonitorListener(HWatchdogPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.sessionManager = plugin.getOpSessionManager();
        this.discordSender = plugin.getDiscordWebhookSender();
        this.fileLogger = plugin.getFileLogger();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            sessionManager.startSession(player);
            player.sendMessage(Component.text(":3 [H-Watchdog] You're being monitored. Use /reason to explain your actions.", NamedTextColor.YELLOW));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().isOp()) {
            sessionManager.endSession(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp()) return;

        if (!sessionManager.hasSession(player)) {
            sessionManager.startSession(player);
        }

        String command = event.getMessage().split(" ")[0].toLowerCase();
        if (!config.getWatchedCommands().contains(command)) return;

        OPSession session = sessionManager.getSession(player);
        if (session == null) return;

        fileLogger.logCommand(player, event.getMessage());

        if (config.getExcludedOps().contains(player.getName().toLowerCase())) {
            return;
        }

        String reason = session.getReason(config.getReasonExpirySeconds());
        session.clearReason();

        if (config.isAutoDemoteEnabled()) {
            session.addViolation();
            int violations = session.getRecentViolationCount(config.getAutoDemoteCooldown());
            if (violations >= config.getAutoDemoteThreshold()) {
                Bukkit.getScheduler().runTask(plugin, () -> player.setOp(false));

                discordSender.sendAutoDemoteAlert(player.getName());
                plugin.getLogger().warning(player.getName() + " was auto-demoted.");
                event.setCancelled(true);
                return;
            }
        }

        discordSender.sendPublicCommandAlert(player, event.getMessage(), reason);
    }
}