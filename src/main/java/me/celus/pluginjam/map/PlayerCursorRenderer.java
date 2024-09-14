package me.celus.pluginjam.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import me.celus.pluginjam.game.Game;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

public class PlayerCursorRenderer extends MapRenderer {

    private static final double BLOCKS_PER_PIXEL = (Game.GAME_WIDTH * 16) / 256d;
    private static final long PLAYER_UPDATE_INTERVAL = 10;

    private final Map<UUID, Map<UUID, MapCursor>> cursorMap = new HashMap<>();
    private final Game game;

    public PlayerCursorRenderer(Game game) {
        super(true);
        this.game = game;
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        Location center = map.getWorld().getSpawnLocation();
        boolean shouldUpdatePlayers = (System.currentTimeMillis() / (1000 / 4)) % (PLAYER_UPDATE_INTERVAL * 4) == 0;

        MapCursorCollection cursors = canvas.getCursors();
        List<Game.Participant> participants = game.getParticipants();
        for (Game.Participant participant : participants) {
            Player participantPlayer = participant.getPlayer();
            boolean isOwner = participantPlayer == player;

            MapCursor cursor = getCursor(cursors, player.getUniqueId(), participantPlayer.getUniqueId(), cur -> {
                cur.setType(isOwner ? MapCursor.Type.PLAYER : MapCursor.Type.PLAYER_OFF_LIMITS);
                if (!isOwner) {
                    cur.caption(Component.text(participantPlayer.getName()));
                }
            });
            if (!participant.isAlive()) {
                if (participant.getDeathLocation() == null) {
                    continue;
                }
                cursor.setType(MapCursor.Type.TARGET_X);
                cursor.setDirection((byte) 0);
                updateCursorPos(cursor, participant.getDeathLocation(), center);
                continue;
            }

            if (!shouldUpdatePlayers && !isOwner) {
                continue;
            }
            updateCursorPos(cursor, participantPlayer, center);
            if (isOwner) {
                updateCursorDir(cursor, player);
            }
        }
    }

    private MapCursor getCursor(MapCursorCollection cursors, UUID owner, UUID uuid, Consumer<MapCursor> modifier) {
        return cursorMap.computeIfAbsent(owner, $ -> new HashMap<>()).computeIfAbsent(uuid, $ -> {
            MapCursor cursor = new MapCursor((byte) 0, (byte) 0, (byte) 0, MapCursor.Type.PLAYER, true);
            modifier.accept(cursor);
            cursors.addCursor(cursor);
            return cursor;
        });
    }

    private void updateCursorPos(MapCursor cursor, Player player, Location center) {
        updateCursorPos(cursor, player.getLocation(), center);
    }

    private void updateCursorPos(MapCursor cursor, Location loc, Location center) {
        double adjX = (loc.getX() - center.getX()) / BLOCKS_PER_PIXEL;
        double adjZ = (loc.getZ() - center.getZ()) / BLOCKS_PER_PIXEL;
        adjX = Math.min(127, Math.max(-128, adjX));
        adjZ = Math.min(127, Math.max(-128, adjZ));
        cursor.setX((byte) adjX);
        cursor.setY((byte) adjZ);
    }

    private void updateCursorDir(MapCursor cursor, @NotNull Player player) {
        float yaw = player.getYaw() + 180;
        byte adjYaw = (byte) (((yaw / 360f) * 16 + 8) % 16);
        cursor.setDirection(adjYaw);
    }
}
