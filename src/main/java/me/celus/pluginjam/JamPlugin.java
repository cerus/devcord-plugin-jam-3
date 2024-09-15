package me.celus.pluginjam;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import me.celus.pluginjam.command.SetDelayCommand;
import me.celus.pluginjam.command.StartGameCommand;
import me.celus.pluginjam.feature.*;
import me.celus.pluginjam.game.Game;
import me.celus.pluginjam.game.GameState;
import me.celus.pluginjam.game.state.WaitingState;
import me.celus.pluginjam.listener.PlayerJoinListener;
import me.celus.pluginjam.util.PacketInjector;
import me.celus.pluginjam.util.TextureUtil;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.apache.commons.io.FileUtils;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class JamPlugin extends JavaPlugin {

    private final Set<Feature> features = new HashSet<>();
    private Component developerMax;
    private Component developerLukas;
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
        new File(worldDir, "data").mkdirs();

        File dpDir = new File(worldDir, "datapacks/celus");
        File lootTableDir = new File(dpDir, "data/minecraft/loot_table/chests");
        File villageLootTableDir = new File(lootTableDir, "village");
        lootTableDir.mkdirs();

        save("pack.mcmeta", dpDir);
        save("buried_treasure.json", lootTableDir);
        save("jungle_temple_dispenser.json", lootTableDir);
        save("village_temple.json", villageLootTableDir);
    }

    private void save(String path, File dir) {
        try (InputStream in = JamPlugin.class.getResourceAsStream("/datapack/" + path)) {
            byte[] bytes = in.readAllBytes();
            String[] split = path.split("/");
            FileUtils.writeByteArrayToFile(new File(dir, split[split.length - 1]), bytes);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to save datapack", e);
        }
    }

    @Override
    public void onEnable() {
        //ServerApi api = getPlugin(PluginJam.class).api();
        // api.requestRestart();

        World world = getServer().getWorld("world");
        world.setSpawnLocation(8, 0, 8);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerJoinListener(), this);

        newGame(null);

        registerFeature(new SheepExplosionFeature());
        registerFeature(new ArrowHitShowsCreditsFeature());
        registerFeature(new EntityScalingFeature());
        registerFeature(new SunGravityFeature());
        registerFeature(new FluidSwitchFeature());
        registerFeature(new PortalFeature());
        registerFeature(new ItemFloatFeature());
        registerFeature(new TooManyArrowsFeature());
        registerFeature(new EndermanStealsItemFeature());

        getCommand("startgame").setExecutor(new StartGameCommand(this));
        getCommand("setdelay").setExecutor(new SetDelayCommand(this));

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

        CompletableFuture.allOf(
                TextureUtil.getHeadComponent(UUID.fromString("06f8c3cc-a3c5-4b48-bc6d-d3ee8963f2af")).thenAccept(component -> developerMax = component),
                TextureUtil.getHeadComponent(UUID.fromString("4c4009fd-117b-47e1-98bc-5f4a37f42db3")).thenAccept(component -> developerLukas = component)
        ).thenAcceptAsync($ -> newGame(), getServer().getScheduler().getMainThreadExecutor(this));
    }

    public void newGame() {
        newGame(new WaitingState());
    }

    public void newGame(GameState initialState) {
        if (game != null) {
            game.stop();
            features.forEach(f -> f.onGameDestroyed(game));
        }
        getLogger().info("Starting new game");
        World world = getServer().getWorld("world");
        game = new Game(this, world);
        if (initialState != null) {
            game.init();
            game.start(initialState);
        }
        getServer().getPluginManager().registerEvents(game, this);
        features.forEach(f -> f.onGameSpawned(game));
    }

    private void registerFeature(Feature feature) {
        getServer().getPluginManager().registerEvents(feature, this);
        feature.onRegister(this);
        features.add(feature);
    }

    public Game getGame() {
        return game;
    }

    public Component getDeveloperMax() {
        return developerMax;
    }

    public Component getDeveloperLukas() {
        return developerLukas;
    }
}
