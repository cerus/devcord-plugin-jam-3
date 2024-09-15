package me.celus.pluginjam.feature;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import me.celus.pluginjam.JamPlugin;
import me.celus.pluginjam.game.Game;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.scheduler.BukkitRunnable;

public class FluidSwitchFeature implements Feature {

    private final Predicate<BlockData> waterPredicate = blockState -> blockState.getMaterial() == Material.WATER
                                                                      || (blockState instanceof Waterlogged wl) && wl.isWaterlogged()
                                                                      || blockState.getMaterial() == Material.SEAGRASS
                                                                      || blockState.getMaterial() == Material.TALL_SEAGRASS
                                                                      || blockState.getMaterial() == Material.ICE;
    private final Predicate<BlockData> lavaPredicate = blockState -> blockState.getMaterial() == Material.LAVA;
    private final Map<Long, Set<BlockState>> chunkLavaStates = new HashMap<>();
    private boolean fluidToggle;

    @Override
    public void onRegister(JamPlugin plugin) {
        int delayBetweenChanges = ThreadLocalRandom.current().nextInt(60, 60 * 3) * 20;
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            updateWorld(plugin);
        }, delayBetweenChanges, delayBetweenChanges);
    }

    private void updateWorld(JamPlugin plugin) {
        Game game = plugin.getGame();
        World world = game.getWorld();
        Chunk spawnChunk = world.getSpawnLocation().getChunk();
        int w = Game.GAME_WIDTH / 2 + 1;
        fluidToggle = !fluidToggle;

        new BukkitRunnable() {
            private int cx = -w;
            private int cz = -w;

            @Override
            public void run() {
                if (cz >= w) {
                    cancel();
                    return;
                }

                while (cx < w) {
                    Chunk chunk = world.getChunkAt(cx + spawnChunk.getX(), cz + spawnChunk.getZ());
                    FluidSwitchFeature.this.updateChunk(chunk, fluidToggle ? waterPredicate : lavaPredicate, fluidToggle ? Material.LAVA : Material.WATER);
                    cx++;
                }
                cx = -w;
                cz++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void updateChunk(Chunk chunk, Predicate<BlockData> predicate, Material replaceWith) {
        CraftBlockData replaceWithData = (CraftBlockData) replaceWith.createBlockData();
        int maxHeight = chunk.getWorld().getMaxHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < maxHeight; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    BlockData oldData = block.getBlockData();
                    if (!predicate.test(oldData)) {
                        continue;
                    }
                    block.setType(replaceWith, false);
                    BlockData newData = block.getBlockData();
                    if (oldData instanceof Levelled old && newData instanceof Levelled n) {
                        n.setLevel(old.getLevel());
                        block.setBlockData(n, false);
                    }
                }
            }
        }
        //Bukkit.broadcast(Component.text("DEBUG: " + chunk.getX() + "," + chunk.getZ()));
    }
}
