package me.celus.pluginjam.util;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PacketInjector {
    private static final Map<Class<?>, List<PacketHandler>> typedOutboundHandlers = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<PacketHandler>> typedInboundHandlers = new ConcurrentHashMap<>();

    private PacketInjector() {
        throw new UnsupportedOperationException();
    }

    public static <T extends Packet<?>> void registerOutboundHandler(Class<T> packetClass, PacketHandler<T> handler) {
        typedOutboundHandlers.computeIfAbsent(packetClass, $ -> new ArrayList<>()).add(handler);
    }

    public static <T extends Packet<?>> void registerInboundHandler(Class<T> packetClass, PacketHandler<T> handler) {
        typedInboundHandlers.computeIfAbsent(packetClass, $ -> new ArrayList<>()).add(handler);
    }

    public static void inject(Player player) {
        ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        ChannelPipeline pipeline = nmsPlayer.connection.connection.channel.pipeline();
        pipeline.addBefore("packet_handler", "pluginjam", new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof Packet<?> packet && handlePacket(typedOutboundHandlers, player, packet)) {
                    return;
                }
                super.write(ctx, msg, promise);
            }

            @Override
            public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
                if (msg instanceof Packet<?> packet && handlePacket(typedInboundHandlers, player, packet)) {
                    return;
                }
                super.channelRead(ctx, msg);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean handlePacket(Map<Class<?>, List<PacketHandler>> handlers, Player player, Packet<?> packet) {
        for (PacketHandler handler : handlers.get(packet.getClass())) {
            if (!handler.handle(player, packet)) {
                return true;
            }
        }
        return false;
    }
}
