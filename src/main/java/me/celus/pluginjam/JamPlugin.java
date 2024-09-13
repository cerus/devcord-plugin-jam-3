package me.celus.pluginjam;


import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import me.celus.pluginjam.feature.Feature;
import me.celus.pluginjam.feature.SheepExplosionFeature;
import me.celus.pluginjam.listener.ElytraListener;
import me.celus.pluginjam.listener.PlayerJoinListener;
import me.celus.pluginjam.util.PacketInjector;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.apache.commons.io.FileUtils;
import org.bukkit.Chunk;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class JamPlugin extends JavaPlugin {

    public static final int GAME_WIDTH = 21;
    private static final int SPAWN_X = 8;
    private static final int SPAWN_Z = 8;

    private World gameWorld;

    @Override
    public void onLoad() {
        File worldDir = new File(getServer().getWorldContainer(), "world");
        try {
            FileUtils.deleteDirectory(worldDir);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to delete game world", e);
        }

        new File(worldDir, "playerdata").mkdirs();
        new File(worldDir, "datapacks").mkdirs();
        new File(worldDir, "data").mkdirs();
    }

    @Override
    public void onEnable() {
        //ServerApi api = getPlugin(PluginJam.class).api();
        // api.requestRestart();

        PacketInjector.registerOutboundHandler(ClientboundLevelChunkWithLightPacket.class, (player, packet) -> {
            int adjustedWidth = (GAME_WIDTH / 2) + 1;
            return Math.abs(packet.getX()) <= adjustedWidth && Math.abs(packet.getZ()) <= adjustedWidth;
        });

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerJoinListener(this), this);
        pluginManager.registerEvents(new ElytraListener(), this);

        gameWorld = getServer().getWorld("world");
        gameWorld.setGameRule(GameRule.SPAWN_RADIUS, 0);
        WorldBorder worldBorder = gameWorld.getWorldBorder();
        worldBorder.setCenter(SPAWN_X, SPAWN_Z);
        worldBorder.setSize(16 * GAME_WIDTH);

        generateSpawnBox(Material.BEDROCK, Material.GLASS);

        gameWorld.setSpawnLocation(SPAWN_X, gameWorld.getHighestBlockYAt(SPAWN_X, SPAWN_Z) + 1, SPAWN_Z);

        registerFeature(new SheepExplosionFeature());
    }

    public void generateSpawnBox(Material wallMat, Material floorMat) {
        int spawnLayer = gameWorld.getMaxHeight() - 8;
        Chunk chunkZero = gameWorld.getChunkAt(0, 0);
        for (int y = spawnLayer; y < gameWorld.getMaxHeight(); y++) {
            for (int i = 0; i < 16; i++) {
                chunkZero.getBlock(i, y, 0).setType(wallMat);
                chunkZero.getBlock(i, y, 15).setType(wallMat);
                chunkZero.getBlock(0, y, i).setType(wallMat);
                chunkZero.getBlock(15, y, i).setType(wallMat);
            }
        }
        for (int x = 1; x < 15; x++) {
            for (int z = 1; z < 15; z++) {
                chunkZero.getBlock(x, spawnLayer, z).setType(floorMat);
            }
        }
    }

    public World getGameWorld() {
        return gameWorld;
    }

    private void registerFeature(Feature feature) {
        getServer().getPluginManager().registerEvents(feature, this);
        feature.onRegister(this);
    }
}
