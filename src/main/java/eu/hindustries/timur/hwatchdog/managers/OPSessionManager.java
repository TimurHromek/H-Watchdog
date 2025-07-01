package eu.hindustries.timur.hwatchdog.managers;

import eu.hindustries.timur.hwatchdog.HWatchdogPlugin;
import eu.hindustries.timur.hwatchdog.model.OPSession;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OPSessionManager {
    private final HWatchdogPlugin plugin;
    private final ConcurrentHashMap<UUID, OPSession> activeSessions = new ConcurrentHashMap<>();

    public OPSessionManager(HWatchdogPlugin plugin) {
        this.plugin = plugin;
    }

    public void startSession(Player player) {
        activeSessions.put(player.getUniqueId(), new OPSession(player.getUniqueId()));
    }

    public void endSession(Player player) {
        activeSessions.remove(player.getUniqueId());
    }

    public OPSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public boolean hasSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
}
