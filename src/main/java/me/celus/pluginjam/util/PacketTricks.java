package me.celus.pluginjam.util;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class PacketTricks {

    private PacketTricks() {
        throw new UnsupportedOperationException();
    }

    public static void showDemoScreen(Player player) {
        ClientboundGameEventPacket packet = new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 0);
        sendPacket(player, packet);
    }

    public static void showCredits(Player player) {
        ClientboundGameEventPacket packet = new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 1);
        sendPacket(player, packet);
    }

    public static void playHurtAnimation(Player player, Entity entity) {
        sendPacket(player, new ClientboundHurtAnimationPacket(entity.getEntityId(), 0));
    }

    public static void sendPacket(Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.sendPacket(packet);
    }
}
