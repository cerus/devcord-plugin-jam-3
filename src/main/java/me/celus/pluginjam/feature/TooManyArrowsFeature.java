package me.celus.pluginjam.feature;

import me.celus.pluginjam.JamPlugin;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;

public class TooManyArrowsFeature implements Feature {

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof Arrow)) {
            return;
        }
        player.setArrowsInBody(100);
        player.setNextArrowRemoval(20 * 60);
    }

    @Override
    public void onRegister(JamPlugin plugin) {

    }
}
