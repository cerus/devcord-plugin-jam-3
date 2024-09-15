package me.celus.pluginjam.feature;

import java.util.HashSet;
import java.util.Set;
import me.celus.pluginjam.JamPlugin;
import me.celus.pluginjam.util.PacketTricks;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class SheepExplosionFeature implements Feature {

    private JamPlugin plugin;
    private Set<Integer> primedSheep;

    @Override
    public void onRegister(JamPlugin plugin) {
        this.plugin = plugin;
        primedSheep = new HashSet<>();
    }

    @EventHandler
    public void onSheepShear(PlayerShearEntityEvent event) {
        Entity entity = event.getEntity();

        int id = entity.getEntityId();
        if (primedSheep.contains(id)) {
            return;
        }
        primedSheep.add(id);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1, 1);

        int durationTicks = 80;
        new BukkitRunnable() {
            private int ticks = 0;
            private int delay = durationTicks / 3;

            private int nextRunningTick = 0;

            @Override
            public void run() {
                if (!entity.isValid()) {
                    this.cancel();
                    return;
                }

                if (ticks == nextRunningTick) {
                    plugin.getServer().getOnlinePlayers().forEach(player -> PacketTricks.playHurtAnimation(player, entity));
                    nextRunningTick = ticks + delay;
                    delay -= 5;
                }

                ticks++;
                if (ticks == durationTicks) {
                    plugin.getServer().getScheduler().callSyncMethod(plugin, () -> entity.getWorld().createExplosion(entity.getLocation(), 5));
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 1);
    }

    @EventHandler
    public void onEntityDie(EntityDeathEvent event) {
        primedSheep.remove(event.getEntity().getEntityId());
    }

}
