package eu.hindustries.timur.hwatchdog.commands;

import eu.hindustries.timur.hwatchdog.HWatchdogPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReportOPCommand implements CommandExecutor {
    private final HWatchdogPlugin plugin;
    private final ConcurrentHashMap<UUID, Long> reportCooldowns = new ConcurrentHashMap<>();

    public ReportOPCommand(HWatchdogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        int cooldownSeconds = plugin.getConfigManager().getReportCooldownSeconds();
        if (reportCooldowns.containsKey(player.getUniqueId())) {
            long secondsLeft = ((reportCooldowns.get(player.getUniqueId()) / 1000) + cooldownSeconds) - (System.currentTimeMillis() / 1000);
            if (secondsLeft > 0) {
                player.sendMessage(Component.text("You must wait " + secondsLeft + " more seconds before reporting again.", NamedTextColor.RED));
                return true;
            }
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /reportop <player> <reason...>", NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.isOp()) {
            player.sendMessage(Component.text(target.getName() + " is not an operator.", NamedTextColor.RED));
            return true;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        plugin.getDiscordWebhookSender().sendReportAlert(player.getName(), target.getName(), reason);

        player.sendMessage(Component.text("Your report has been sent to the server staff. Thank you.", NamedTextColor.GREEN));
        reportCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        return true;
    }
}