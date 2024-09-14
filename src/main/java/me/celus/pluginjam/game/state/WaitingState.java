package me.celus.pluginjam.game.state;

import me.celus.pluginjam.game.GameState;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

public class WaitingState extends GameState {

    private static final int MIN_PLAYERS = 1;
    private static final int START_DELAY = 20 * 20 + 1;
    private int delay;

    @Override
    public void tick(int totalElapsedTicks) {
        World gameWorld = getGame().getWorld();
        if (gameWorld.getPlayerCount() < MIN_PLAYERS) {
            delay = START_DELAY;
            return;
        } else {
            delay--;
        }

        if (delay % 20 == 0) {
            int secondsLeft = delay / 20;
            if (secondsLeft > 0 && (secondsLeft <= 5 || secondsLeft % 10 == 0)) {
                gameWorld.getPlayers().forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1));
            }
        }
        if (totalElapsedTicks % 5 == 0) {
            gameWorld.getPlayers().forEach(player -> player.sendActionBar(Component.text("Spiel startet in %d Sekunden".formatted(delay / 20))));
        }
    }

    @Override
    public void onStart() {
        delay = START_DELAY;
        forEachPlayerInGame(this::setupPlayer);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setupPlayer(player);
    }

    private void setupPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setExp(0);
        player.setLevel(0);
        player.teleport(getGame().getWorld().getSpawnLocation());

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        mapItem.editMeta(MapMeta.class, mapMeta -> mapMeta.setMapView(getGame().getMapView()));
        player.getInventory().setItemInOffHand(mapItem);
    }

    @Override
    public boolean shouldContinue() {
        return delay > 0;
    }

    @Override
    public GameState getNextState() {
        return new PlayState();
    }
}
