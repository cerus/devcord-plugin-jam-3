package me.celus.pluginjam.feature;

import me.celus.pluginjam.JamPlugin;
import me.celus.pluginjam.game.Game;
import org.bukkit.event.Listener;

public interface Feature extends Listener {

    void onRegister(JamPlugin plugin);

    default void onGameSpawned(Game game) {
    }

    default void onGameDestroyed(Game game) {
    }

}
