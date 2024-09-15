package me.celus.pluginjam.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
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

    public static void sendTeleportPacket(Player receiver, Entity entity, Location to) {
        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        Vec3 position = new Vec3(handle.position().x, handle.position().y, handle.position().z);
        handle.setPos(to.getX(), to.getY(), to.getZ());
        ClientboundTeleportEntityPacket packet = new ClientboundTeleportEntityPacket(handle);
        handle.setPos(position.x, position.y, position.z);
        sendPacket(receiver, packet);
    }

    public static void sendEntityMetaPacket(Player receiver, Entity entity) {
        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(entity.getEntityId(), handle.getEntityData().packAll());
        sendPacket(receiver, packet);
    }

    public static void sendEntityRemovePacket(Player receiver, Entity entity) {
        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();
        ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entity.getEntityId());
        sendPacket(receiver, packet);
    }

    public static void sendContainerLidPacket(Player player, Block container, boolean open) {
        ClientboundBlockEventPacket packet = new ClientboundBlockEventPacket(new BlockPos(container.getX(), container.getY(), container.getZ()), CraftMagicNumbers.getBlock(container.getType()), 1, open ? 1 : 0);
        sendPacket(player, packet);
    }

    public static void sendPacket(Player player, Packet<?> packet) {
        sendPacket(((CraftPlayer) player).getHandle(), packet);
    }

    public static void sendPacket(ServerPlayer player, Packet<?> packet) {
        player.connection.sendPacket(packet);
    }

}
