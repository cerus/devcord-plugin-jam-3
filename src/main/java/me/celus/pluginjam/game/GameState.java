package me.celus.pluginjam.game;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import me.celus.pluginjam.JamPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public abstract class GameState implements Listener {

    private JamPlugin plugin;
    private Game game;

    public abstract void tick(int totalElapsedTicks);

    public void onStart() {
    }

    public void onEnd() {
    }

    public abstract boolean shouldContinue();

    public abstract GameState getNextState();

    protected void broadcastTitle(Component main, Component sub) {
        forEachPlayerInGame(player -> player.showTitle(Title.title(main, sub)));
    }

    protected void broadcast(Component component) {
        forEachPlayerInGame(player -> player.sendMessage(component));
    }

    protected void forEachPlayerInGame(Consumer<Player> action) {
        getGame().getWorld().getPlayers().forEach(action);
    }

    protected Set<Player> getAlivePlayers() {
        return getGame().getWorld().getPlayers().stream()
                .filter(player -> player.getGameMode() != GameMode.SPECTATOR)
                .filter(player -> !player.isDead())
                .collect(Collectors.toSet());
    }

    protected final JamPlugin getPlugin() {
        return plugin;
    }

    final void setPlugin(JamPlugin plugin) {
        this.plugin = plugin;
    }

    protected final Game getGame() {
        return game;
    }

    final void setGame(Game game) {
        this.game = game;
    }
}
