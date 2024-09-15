package me.celus.pluginjam.feature;

import me.celus.pluginjam.JamPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ItemSpawnEvent;

public class ItemFloatFeature implements Feature {

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        //event.getEntity().setFrictionState(TriState.FALSE);
        event.getEntity().setGravity(false);
    }

    @Override
    public void onRegister(JamPlugin plugin) {
    }
}
