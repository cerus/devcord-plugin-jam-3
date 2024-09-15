package me.celus.pluginjam.feature;

import com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent;
import me.celus.pluginjam.JamPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class EndermanStealsItemFeature implements Feature {

    private final Map<Integer, ItemStack> stolenItems = new HashMap<>();

    @Override
    public void onRegister(JamPlugin plugin) {
    }

    @EventHandler
    public void onEndermanDecidesAttack(EndermanAttackPlayerEvent event) {
        if (!event.isCancelled() && stolenItems.containsKey(event.getEntity().getEntityId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEndermanAttackPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Enderman enderman) || !(event.getEntity() instanceof Player player)) {
            return;
        }

        if (stolenItems.containsKey(enderman.getEntityId())) {
            event.setCancelled(true);
            enderman.setTarget(null);
            return;
        }

        List<Integer> nonNullItems = new ArrayList<>();
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null) {
                continue;
            }
            nonNullItems.add(i);
        }
        Collections.shuffle(nonNullItems);
        if (nonNullItems.isEmpty()) {
            return;
        }

        int slot = nonNullItems.getFirst();
        Material item = contents[slot].getType();

        event.setCancelled(true);
        enderman.setCarriedBlock(item.isBlock() ? item.createBlockData() : Material.CHEST.createBlockData());
        stolenItems.put(enderman.getEntityId(), contents[slot]);
        player.getInventory().setItem(slot, null);
        enderman.setTarget(null);
        enderman.setHasBeenStaredAt(false);
        enderman.setScreaming(false);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Enderman enderman)) {
            return;
        }
        if (stolenItems.containsKey(enderman.getEntityId())) {
            ItemStack item = stolenItems.get(enderman.getEntityId());
            enderman.getWorld().dropItem(enderman.getLocation(), item);
            stolenItems.remove(enderman.getEntityId());
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Enderman && stolenItems.containsKey(event.getEntity().getEntityId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Enderman && stolenItems.containsKey(event.getEntity().getEntityId())) {
            event.setCancelled(true);
        }
    }

}
