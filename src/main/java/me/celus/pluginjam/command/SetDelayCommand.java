package me.celus.pluginjam.command;

import me.celus.pluginjam.JamPlugin;
import me.celus.pluginjam.game.state.WaitingState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SetDelayCommand implements CommandExecutor {

    private final JamPlugin plugin;

    public SetDelayCommand(JamPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Component.text("/setdelay <delay>").color(NamedTextColor.RED));
            return false;
        }
        if (!(plugin.getGame().getCurrentState() instanceof WaitingState ws)) {
            sender.sendMessage(Component.text("Spiel läuft bereits").color(NamedTextColor.RED));
            return false;
        }
        int delay = Integer.parseInt(args[0]);
        ws.setDelay(Math.max(1, delay) * 20 + 1);
        sender.sendMessage(Component.text("Wartezeit wurde gesetzt").color(NamedTextColor.GREEN));
        return false;
    }
}
