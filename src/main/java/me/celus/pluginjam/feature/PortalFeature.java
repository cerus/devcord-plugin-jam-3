package me.celus.pluginjam.feature;

import me.celus.pluginjam.JamPlugin;
import me.celus.pluginjam.util.PacketTricks;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PortalFeature implements Feature {

    @EventHandler
    public void onPortalEnter(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        Player randomPlayer = player.getWorld().getPlayers().stream()
                .filter(p -> p != player)
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .findAny().orElse(null);
        event.setCancelled(true);
        if (randomPlayer != null) {
            player.teleport(randomPlayer.getLocation());
            randomPlayer.getWorld().playSound(randomPlayer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
        } else {
            PacketTricks.showCredits(player);
        }
    }

    @Override
    public void onRegister(JamPlugin plugin) {
    }
}
