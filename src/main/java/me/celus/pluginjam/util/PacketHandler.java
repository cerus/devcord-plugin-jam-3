package me.celus.pluginjam.util;

import net.minecraft.network.protocol.Packet;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface PacketHandler<T extends Packet<?>> {

    // Return false to cancel
    boolean handle(Player player, T packet);

}
