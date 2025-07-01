package eu.hindustries.timur.hwatchdog.commands;

import eu.hindustries.timur.hwatchdog.HWatchdogPlugin;
import eu.hindustries.timur.hwatchdog.model.OPSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReasonCommand implements CommandExecutor {
    private final HWatchdogPlugin plugin;

    public ReasonCommand(HWatchdogPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(Component.text("You must be an operator to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /reason <text...>", NamedTextColor.RED));
            return true;
        }

        OPSession session = plugin.getOpSessionManager().getSession(player);
        if (session == null) {
            player.sendMessage(Component.text("Could not find your OP session. Please rejoin.", NamedTextColor.RED));
            return true;
        }

        String reason = String.join(" ", args);
        session.setReason(reason);

        player.sendMessage(Component.text("Reason set. It will be attached to your next monitored command.", NamedTextColor.GREEN));
        return true;
    }
}