package me.celus.pluginjam.listener;

import me.celus.pluginjam.JamPlugin;
import me.celus.pluginjam.util.PacketInjector;
import me.celus.pluginjam.util.PacketTricks;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerJoinListener implements Listener {

    private final JamPlugin plugin;

    public PlayerJoinListener(JamPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.teleport(plugin.getGameWorld().getSpawnLocation());
        player.getEquipment().setChestplate(new ItemStack(Material.ELYTRA));

        PacketInjector.inject(player);
        PacketTricks.showDemoScreen(player);
    }

}
