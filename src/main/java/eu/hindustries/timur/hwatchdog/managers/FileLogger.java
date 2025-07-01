package eu.hindustries.timur.hwatchdog.managers;

import eu.hindustries.timur.hwatchdog.HWatchdogPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogger {
    // ... (Code is identical, only imports and package changed)
    private final HWatchdogPlugin plugin;
    private final Path logDirectory;
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public FileLogger(HWatchdogPlugin plugin) {
        this.plugin = plugin;
        this.logDirectory = plugin.getDataFolder().toPath().resolve("logs");
        try {
            Files.createDirectories(logDirectory);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create log directory!");
            e.printStackTrace();
        }
    }

    public void logCommand(Player player, String command) {
        String timestamp = LocalDateTime.now().format(timestampFormatter);
        Location loc = player.getLocation();

        String logEntry = String.format("[%s] OP: %s | Command: %s",
                timestamp,
                player.getName(),
                command
        );

        if (plugin.getConfigManager().includeCoordinatesInLogs()) {
            logEntry += String.format(" | Location: %s at X:%.1f Y:%.1f Z:%.1f",
                    loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
        }

        logEntry += "\n";

        final String finalLogEntry = logEntry;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Path logFile = logDirectory.resolve(LocalDate.now().format(dateFormatter) + ".log");
                Files.writeString(logFile, finalLogEntry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to write to log file: " + e.getMessage());
            }
        });
    }
}