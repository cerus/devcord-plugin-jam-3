package me.celus.pluginjam.feature;

import me.celus.pluginjam.JamPlugin;
import me.celus.pluginjam.util.PacketTricks;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.attribute.CraftAttribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

public class EntityScalingFeature implements Feature {

    private final HashMap<UUID, HashMap<Integer, Double>> previousScales = new HashMap<>();

    @Override
    public void onRegister(JamPlugin plugin) {
        CraftWorld world = (CraftWorld) plugin.getGame().getWorld();

        new BukkitRunnable(){
            @Override
            public void run() {
                world.getHandle().getChunkSource().chunkMap.entityMap.forEach((id, trackedEntity) -> {
                    Entity entity = world.getHandle().getEntity(id);
                    if (!(entity instanceof LivingEntity) || entity instanceof Player) {
                        return;
                    }

                    Location entityLocation = entity.getBukkitEntity().getLocation();

                    trackedEntity.seenBy.forEach(connection -> {
                        Location playerLocation = connection.getPlayer().getBukkitEntity().getLocation();
                        double distance = entityLocation.distance(playerLocation);

                        double scale = Math.min(distance, 20) / 20 * 10;

                        HashMap<Integer, Double> previousPlayerScales = previousScales.computeIfAbsent(connection.getPlayer().getBukkitEntity().getUniqueId(), $ -> new HashMap<>());
                        Double previousScale = previousPlayerScales.get(id);
                        if (previousScale != null && previousScale == scale) {
                            return;
                        }

                        AttributeInstance attr = new AttributeInstance(CraftAttribute.bukkitToMinecraftHolder(Attribute.GENERIC_SCALE), $ -> {});
                        attr.setBaseValue(scale);
                        previousPlayerScales.put(id, scale);
                        PacketTricks.sendPacket(connection.getPlayer(), new ClientboundUpdateAttributesPacket(id, Collections.singletonList(attr)));
                    });
                });
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        previousScales.remove(event.getPlayer().getUniqueId());
    }

}
