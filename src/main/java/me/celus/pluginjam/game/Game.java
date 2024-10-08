package me.celus.pluginjam.game;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import me.celus.pluginjam.JamPlugin;
import me.celus.pluginjam.map.PaintedWorldRenderer;
import me.celus.pluginjam.util.MapUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Game implements Listener {

    public static final int GAME_WIDTH = 21;
    public static final double UNDESIRED_BIOME_THRESHOLD = 0.25;
    private static final EnumSet<Biome> UNDESIRED_BIOMES = EnumSet.of(Biome.OCEAN, Biome.COLD_OCEAN, Biome.DEEP_OCEAN, Biome.DEEP_COLD_OCEAN,
            Biome.WARM_OCEAN, Biome.LUKEWARM_OCEAN, Biome.FROZEN_OCEAN, Biome.DEEP_LUKEWARM_OCEAN, Biome.RIVER, Biome.FROZEN_RIVER,
            Biome.DEEP_FROZEN_OCEAN, Biome.BADLANDS, Biome.ERODED_BADLANDS, Biome.SNOWY_PLAINS);

    private final List<Participant> participants = new ArrayList<>();
    private final JamPlugin plugin;
    private final World world;
    private MapView mapView;
    private GameState currentState;
    private BukkitTask gameTask;
    private int elapsedTicks;

    public Game(JamPlugin plugin, World world) {
        this.plugin = plugin;
        this.world = world;
    }

    public void init() {
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setDifficulty(Difficulty.NORMAL);
        world.setTime(0);

        Location spawnLoc = findSuitableSpawn(world);
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setCenter(spawnLoc.getX(), spawnLoc.getZ());
        worldBorder.setSize(16 * GAME_WIDTH);
        worldBorder.setDamageBuffer(0);

        world.setSpawnLocation(spawnLoc.getBlockX(), (world.getMaxHeight() - 8) + 1, spawnLoc.getBlockZ());

        byte[] paintedWorld = new byte[128 * 128];
        mapView = plugin.getServer().createMap(world);
        mapView.getRenderers().forEach(mapView::removeRenderer);
        mapView.addRenderer(new PaintedWorldRenderer(paintedWorld));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getLogger().info("Generating game map... This can take a while.");
            try {
                MapUtil.paintWorld(world, paintedWorld);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Failed to generate map", t);
            }
            plugin.getLogger().info("Finished generating game map.");
        });

        generateSpawnBox(Material.BARRIER, Material.BARRIER);
    }

    public void start(GameState firstState) {
        plugin.getLogger().info("Starting game");
        getWorld().getPlayers().forEach(player -> participants.add(new Participant(player)));
        setState(firstState);
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void tick() {
        if (currentState == null) {
            return;
        }
        currentState.tick(elapsedTicks++);
        if (currentState.shouldContinue()) {
            return;
        }
        setState(currentState.getNextState());
    }

    private void setState(GameState state) {
        if (currentState != null) {
            currentState.onEnd();
            HandlerList.unregisterAll(currentState);
        }
        currentState = state;
        if (currentState == null) {
            plugin.newGame();
            return;
        }
        currentState.setGame(this);
        currentState.setPlugin(plugin);
        currentState.onStart();
        Bukkit.getPluginManager().registerEvents(currentState, plugin);
    }

    public void stop() {
        plugin.getLogger().info("Disposing game");
        if (mapView != null) {
            mapView.getRenderers().forEach(mapView::removeRenderer);
        }
        if (gameTask != null) {
            gameTask.cancel();
        }
        HandlerList.unregisterAll(this);
        if (currentState != null) {
            HandlerList.unregisterAll(currentState);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        participants.removeIf(participant -> participant.getPlayer() == event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(player.getWorld().getSpawnLocation());
        participants.add(new Participant(player));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        participants.stream()
                .filter(participant -> participant.getPlayer() == player)
                .findAny()
                .ifPresent(participant -> {
                    participant.setDeathLocation(player.getLocation());
                    player.getWorld().strikeLightningEffect(player.getLocation());

                    int placement = (int) getParticipants().stream()
                            .filter(Participant::isAlive)
                            .count();
                    participant.setPlacement(placement);
                });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.getInventory().clear();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.setGameMode(GameMode.SPECTATOR);
        }, 3);

        Location spawnLoc = player.getWorld().getSpawnLocation().clone();
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc.getBlockX(), spawnLoc.getBlockZ()) + 32);
        spawnLoc.setPitch(90);
        event.setRespawnLocation(spawnLoc);
    }

    private Location findSuitableSpawn(World world) {
        Location loc = world.getSpawnLocation();
        int checks = 1;
        do {
            // I am aware that this message will always get printed at least once no matter how safe the spawn is. However, I do not care.
            plugin.getLogger().info("Found more than %s%% undesired biomes at spawn. Generating new spawn...".formatted((int) (UNDESIRED_BIOME_THRESHOLD * 100)));
            int extra = checks * GAME_WIDTH * 16 * 4;
            loc.add(extra, 0, extra);
        } while (getUndesiredBiomePercentage(loc) > UNDESIRED_BIOME_THRESHOLD);
        return loc;
    }

    private double getUndesiredBiomePercentage(Location center) {
        World world = center.getWorld();
        int oceans = 0;
        int fromX = center.getChunk().getX() - ((GAME_WIDTH / 2) + 1);
        int fromZ = center.getChunk().getZ() - ((GAME_WIDTH / 2) + 1);
        for (int x = 0; x < GAME_WIDTH; x++) {
            int cx = fromX + x;
            for (int z = 0; z < GAME_WIDTH; z++) {
                int cz = fromZ + z;
                Biome biome = world.getBiome(cx * 16 + 8, world.getSeaLevel() - 1, cz * 16 + 8);
                if (UNDESIRED_BIOMES.contains(biome)) {
                    oceans++;
                }
            }
        }
        return (double) oceans / (GAME_WIDTH * GAME_WIDTH);
    }

    public void generateSpawnBox(Material wallMat, Material floorMat) {
        int spawnLayer = world.getMaxHeight() - 8;
        Chunk spawnChunk = world.getSpawnLocation().getChunk();
        for (int y = spawnLayer; y < world.getMaxHeight(); y++) {
            for (int i = 0; i < 16; i++) {
                spawnChunk.getBlock(i, y, 0).setType(wallMat);
                spawnChunk.getBlock(i, y, 15).setType(wallMat);
                spawnChunk.getBlock(0, y, i).setType(wallMat);
                spawnChunk.getBlock(15, y, i).setType(wallMat);
            }
        }
        for (int x = 1; x < 15; x++) {
            for (int z = 1; z < 15; z++) {
                spawnChunk.getBlock(x, spawnLayer, z).setType(floorMat);
            }
        }
    }

    public Participant getParticipant(Player player) {
        return participants.stream()
                .filter(participant -> participant.getPlayer() == player)
                .findAny().orElse(null);
    }

    public World getWorld() {
        return world;
    }

    public MapView getMapView() {
        return mapView;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public static class Participant {

        private final Player player;
        private Location deathLocation;
        private int placement;

        public Participant(Player player) {
            this.player = player;
        }

        public Player getPlayer() {
            return player;
        }

        public Location getDeathLocation() {
            return deathLocation;
        }

        public void setDeathLocation(Location deathLocation) {
            this.deathLocation = deathLocation;
        }

        public int getPlacement() {
            return placement;
        }

        public void setPlacement(int placement) {
            this.placement = placement;
        }

        public boolean isAlive() {
            return player.getGameMode() != GameMode.SPECTATOR;
        }
    }
}
