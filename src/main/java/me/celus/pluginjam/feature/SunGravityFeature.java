package me.celus.pluginjam.feature;

import me.celus.pluginjam.JamPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.scheduler.BukkitRunnable;

public class SunGravityFeature implements Feature {

    @Override
    public void onRegister(JamPlugin plugin) {
        new BukkitRunnable(){
            @Override
            public void run() {
                long time = plugin.getGame().getWorld().getTime();
                double gravity = calculateGravity(time);

                plugin.getGame().getWorld().getLivingEntities().forEach(entity -> {
                    AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_GRAVITY);
                    if (attribute == null) {
                        return;
                    }
                    attribute.setBaseValue(gravity);
                });
            }

            private double calculateGravity(long time) {
                double gravity;
                if (time >= 0 && time <= 6000) {
                    gravity = 0.08 - time / 6000f * 0.06;
                } else if (time > 6000 && time <= 13000) {
                    gravity = 0.02 + (time - 6000) / 7000f * 0.06;
                } else if (time > 13000 && time <= 18000) {
                    gravity = 0.08 + (time - 13000) / 5000f * 0.02;
                } else {
                    gravity = 0.1 - (time - 18000) / 6000f * 0.02;
                }
                return gravity;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

}
