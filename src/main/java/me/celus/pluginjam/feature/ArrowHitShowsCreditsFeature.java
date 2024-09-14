package me.celus.pluginjam.feature;

import me.celus.pluginjam.JamPlugin;
import me.celus.pluginjam.util.PacketTricks;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;

public class ArrowHitShowsCreditsFeature implements Feature {

    @Override
    public void onRegister(JamPlugin plugin) {
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow) || event.getHitEntity() == null) {
            return;
        }

        if (event.getHitEntity() instanceof Player target) {
            PacketTricks.showCredits(target);
        }
        if (event.getEntity().getShooter() instanceof Player shooter) {
            PacketTricks.showCredits(shooter);
        }
    }

}
