package me.celus.pluginjam.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class ElytraListener implements Listener {

    @EventHandler
    public void onLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isGliding()
            || player.getY() >= player.getWorld().getMaxHeight() - 8
            || player.getFallDistance() > 0) {
            return;
        }
        ItemStack chestplate = player.getEquipment().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) {
            return;
        }
        player.getEquipment().setChestplate(null);
    }

}
