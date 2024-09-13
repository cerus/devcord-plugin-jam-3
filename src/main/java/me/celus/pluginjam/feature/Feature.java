package me.celus.pluginjam.feature;

import me.celus.pluginjam.JamPlugin;
import org.bukkit.event.Listener;

public interface Feature extends Listener {

    void onRegister(JamPlugin plugin);

}
