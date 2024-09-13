package me.celus.pluginjam.listener;

import me.celus.pluginjam.util.PacketInjector;
import me.celus.pluginjam.util.PacketTricks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PacketInjector.inject(event.getPlayer());

        PacketTricks.showDemoScreen(event.getPlayer());
    }

}
