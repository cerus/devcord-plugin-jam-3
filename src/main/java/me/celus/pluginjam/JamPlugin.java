package me.celus.pluginjam;


import de.chojo.pluginjam.PluginJam;
import de.chojo.pluginjam.serverapi.ServerApi;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import me.celus.pluginjam.listener.PlayerJoinListener;
import org.apache.commons.io.FileUtils;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class JamPlugin extends JavaPlugin {

    private World gameWorld;

    @Override
    public void onLoad() {
        for (World world : getServer().getWorlds()) {
            getServer().unloadWorld(world, false);
        }

        File worldContainer = getServer().getWorldContainer();
        File[] worldDirs = worldContainer.listFiles();
        if (worldDirs == null) {
            return;
        }
        for (File worldDir : worldDirs) {
            try {
                FileUtils.deleteDirectory(worldDir);
                getLogger().info("Deleted world " + worldDir.getName());
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Failed to delete world " + worldDir.getName(), e);
            }
        }
    }

    @Override
    public void onEnable() {
        ServerApi api = getPlugin(PluginJam.class).api();
        // api.requestRestart();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerJoinListener(), this);

        gameWorld = getServer().createWorld(WorldCreator.name("world").type(WorldType.NORMAL));
        WorldBorder worldBorder = gameWorld.getWorldBorder();
        worldBorder.setCenter(0, 0);
        worldBorder.setSize(16 * 8);
        gameWorld.setSpawnLocation(0, gameWorld.getHighestBlockYAt(0, 0), 0);
    }

    public World getGameWorld() {
        return gameWorld;
    }
}
