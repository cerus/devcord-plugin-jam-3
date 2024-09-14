package me.celus.pluginjam.util;

import com.google.common.util.concurrent.AtomicDouble;
import me.celus.pluginjam.game.Game;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

public class MapUtil {

    private static final double BLOCKS_PER_PIXEL = (Game.GAME_WIDTH * 16) / 128d;

    public static void paintWorld(World world, byte[] data) {
        AtomicDouble d = new AtomicDouble();
        for (int px = 0; px < 128; px++) {
            d.set(0);
            for (int py = 0; py < 128; py++) {
                byte color = getMapColor(world, px, py, d);
                data[px * 128 + py] = color;
            }
        }
    }

    private static byte getMapColor(World world, int px, int py, AtomicDouble d) {
        Level nmsWorld = ((CraftWorld) world).getHandle();
        int blockX = (int) (world.getSpawnLocation().getX() + (px - 64) * BLOCKS_PER_PIXEL);
        int blockZ = (int) (world.getSpawnLocation().getZ() + (py - 64) * BLOCKS_PER_PIXEL);
        int t = 0;
        int scale = /*(int) Math.ceil(bpp)*/1;
        double e = 0;

        /*Chunk chunk = world.getBlockAt(blockX, 0, blockZ).getChunk();
        chunk.addPluginChunkTicket(JavaPlugin.getPlugin(JamPlugin.class));*/

        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        mutableBlockPos.set(blockX, 0, blockZ);
        BlockPos.MutableBlockPos mutableBlockPos2 = new BlockPos.MutableBlockPos();
        LevelChunk levelChunk = nmsWorld.getChunk(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ));
        int x = levelChunk.getHeight(Heightmap.Types.WORLD_SURFACE, mutableBlockPos.getX(), mutableBlockPos.getZ()) + 1;

        BlockState blockState3;
        if (x <= nmsWorld.getMinBuildHeight() + 1) {
            blockState3 = Blocks.BEDROCK.defaultBlockState();
        } else {
            do {
                mutableBlockPos.setY(--x);
                blockState3 = levelChunk.getBlockState(mutableBlockPos);
            } while (blockState3.getMapColor(nmsWorld, mutableBlockPos) == MapColor.NONE && x > nmsWorld.getMinBuildHeight());

            if (x > nmsWorld.getMinBuildHeight() && !blockState3.getFluidState().isEmpty()) {
                int y = x - 1;
                mutableBlockPos2.set(mutableBlockPos);

                BlockState blockState2;
                do {
                    mutableBlockPos2.setY(y--);
                    blockState2 = levelChunk.getBlockState(mutableBlockPos2);
                    t++;
                } while (y > nmsWorld.getMinBuildHeight() && !blockState2.getFluidState().isEmpty());

                blockState3 = getCorrectStateForFluidBlock(nmsWorld, blockState3, mutableBlockPos);
            }
        }

        e += (double) x / (double) (scale * scale);

        MapColor mapColor = blockState3.getMapColor(nmsWorld, mutableBlockPos);
        MapColor.Brightness brightness;
        if (mapColor == MapColor.WATER) {
            double f = (double) t * 0.1 + (double) (px + py & 1) * 0.2;
            if (f < 0.5) {
                brightness = MapColor.Brightness.HIGH;
            } else if (f > 0.9) {
                brightness = MapColor.Brightness.LOW;
            } else {
                brightness = MapColor.Brightness.NORMAL;
            }
        } else {
            double g = (e - d.get()) * 4.0 / (double) (scale + 4) + ((double) (px + py & 1) - 0.5) * 0.4;
            //Bukkit.getLogger().info("DEBUG: " + px + ", " + py + ": g= " + g + ", e= " + e + ", d= " + d);
            if (g > 0.6) {
                brightness = MapColor.Brightness.HIGH;
            } else if (g < -0.6) {
                brightness = MapColor.Brightness.LOW;
            } else {
                brightness = MapColor.Brightness.NORMAL;
            }
        }
        d.set(e);

        //chunk.removePluginChunkTicket(JavaPlugin.getPlugin(JamPlugin.class));
        return mapColor.getPackedId(brightness);
    }

    private static BlockState getCorrectStateForFluidBlock(Level world, BlockState state, BlockPos pos) {
        FluidState fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !state.isFaceSturdy(world, pos, Direction.UP) ? fluidState.createLegacyBlock() : state;
    }

    private static class WorldPaintContext {

        public double d;
        public double e;
    }

}
