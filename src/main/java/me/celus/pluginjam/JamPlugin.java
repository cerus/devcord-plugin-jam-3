package me.celus.pluginjam;


import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import me.celus.pluginjam.feature.*;
import me.celus.pluginjam.game.Game;
import me.celus.pluginjam.game.state.WaitingState;
import me.celus.pluginjam.listener.PlayerJoinListener;
import me.celus.pluginjam.util.PacketInjector;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.apache.commons.io.FileUtils;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class JamPlugin extends JavaPlugin {

    private Game game;

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

        World world = getServer().getWorld("world");
        world.setSpawnLocation(8, 0, 8);

        newGame();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerJoinListener(), this);

        registerFeature(new SheepExplosionFeature());
        registerFeature(new ArrowHitShowsCreditsFeature());
        registerFeature(new EntityScalingFeature());
        registerFeature(new SunGravityFeature());

        PacketInjector.registerOutboundHandler(ClientboundLevelChunkWithLightPacket.class, (player, packet) -> {
            if (game == null || game.getWorld() == null) {
                return true;
            }
            Chunk spawnChunk = game.getWorld().getSpawnLocation().getChunk();
            int widthPos = (Game.GAME_WIDTH / 2) + 1 + spawnChunk.getX();
            int widthNeg = spawnChunk.getX() + -((Game.GAME_WIDTH / 2) + 1);
            int cx = packet.getX();
            int cz = packet.getZ();
            return cx <= widthPos && cz <= widthPos
                   && cx >= widthNeg && cz >= widthNeg;
        });
    }

    public void newGame() {
        if (game != null) {
            game.stop();
        }
        World world = getServer().getWorld("world");
        game = new Game(this, world);
        game.start(new WaitingState());
        getServer().getPluginManager().registerEvents(game, this);
    }

    public Game getGame() {
        return game;
    }

    private void registerFeature(Feature feature) {
        getServer().getPluginManager().registerEvents(feature, this);
        feature.onRegister(this);
    }

}
