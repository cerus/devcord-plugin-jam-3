package me.celus.pluginjam.command;

import me.celus.pluginjam.JamPlugin;
import me.celus.pluginjam.game.Game;
import me.celus.pluginjam.game.state.WaitingState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class StartGameCommand implements CommandExecutor {

    private final JamPlugin plugin;

    public StartGameCommand(JamPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Game game = plugin.getGame();
        if (!(game.getCurrentState() instanceof WaitingState ws)) {
            sender.sendMessage(Component.text("Spiel läuft bereits").color(NamedTextColor.RED));
            return false;
        }
        ws.forceStart();
        sender.sendMessage(Component.text("Spiel wird gestartet").color(NamedTextColor.GREEN));
        return true;
    }
}
