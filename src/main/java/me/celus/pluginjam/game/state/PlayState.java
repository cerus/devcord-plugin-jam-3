package me.celus.pluginjam.game.state;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import me.celus.pluginjam.game.Game;
import me.celus.pluginjam.game.GameState;
import me.celus.pluginjam.map.GameBorderRenderer;
import me.celus.pluginjam.map.PlayerCursorRenderer;
import me.celus.pluginjam.util.MatrixMath;
import me.celus.pluginjam.util.PacketHandler;
import me.celus.pluginjam.util.PacketInjector;
import me.celus.pluginjam.util.PacketTricks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Lidded;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayState extends GameState {

    private static final boolean DEBUG_NO_FINISH = false;

    private static final List<LootTables> POSSIBLE_LOOT_TABLES = List.of(LootTables.SPAWN_BONUS_CHEST, LootTables.VILLAGE_PLAINS_HOUSE,
            LootTables.SHIPWRECK_SUPPLY, LootTables.SHIPWRECK_TREASURE, LootTables.JUNGLE_TEMPLE_DISPENSER, LootTables.VILLAGE_TEMPLE);
    private static final List<LootTables> TREASURE_LOOT_TABLES = List.of(LootTables.BURIED_TREASURE, LootTables.END_CITY_TREASURE);
    private static final int GAME_LENGTH = 20 * 60 * 10;
    private static final int ZONE_SHRINK_AMOUNT = 4;
    private static final int ZONE_SHRINK_TICKS = GAME_LENGTH / ZONE_SHRINK_AMOUNT;
    private static final int ELYTRA_DURABILITY = 30;
    private static final Object OBJECT = new Object();

    private final Cache<UUID, Object> recentGlidingPlayers = CacheBuilder.newBuilder().expireAfterWrite(500, TimeUnit.MILLISECONDS).build();
    private final Set<UUID> ignoreFallDamage = new HashSet<>();
    private final Set<Long> openContainers = new HashSet<>();
    private PacketHandler<ClientboundRemoveEntitiesPacket> packetHandlerDestroy;
    private PacketHandler<ClientboundBlockEventPacket> packetHandlerBlock;
    private BossBar gameEndBar;
    private BossBar zoneChangeBar;
    private TextDisplay display;
    private int remainingTicks;
    private double borderShrinkAmount;
    private int zoneChanges;
    private Player winner;
    private FinishType finishType;

    @Override
    public void tick(int totalElapsedTicks) {
        remainingTicks--;
        if (remainingTicks % ZONE_SHRINK_TICKS == 0) {
            zoneChanges++;
            WorldBorder border = getGame().getWorld().getWorldBorder();
            border.setSize(Math.max(0, border.getSize() - borderShrinkAmount), (ZONE_SHRINK_TICKS / (4 * zoneChanges)) / 20);
            broadcast(Component.text("Die Zone schrumpft!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
            forEachPlayerInGame(player -> player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.25f, 1));

            if (zoneChanges == ZONE_SHRINK_AMOUNT - 1) {
                broadcastTitle(
                        Component.text("Sudden Death").color(NamedTextColor.RED),
                        Component.text("Überlebe so lange wie möglich!").color(NamedTextColor.GRAY)
                );
                forEachPlayerInGame(player -> {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1);
                    if (player.getGameMode() != GameMode.SPECTATOR) {
                        player.setGlowing(true);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, PotionEffect.INFINITE_DURATION, 1));
                    }
                });
            }
        }
        if (remainingTicks % 5 == 0) {
            gameEndBar.setTitle(formatGameTimeRemaining());
            gameEndBar.setProgress((double) remainingTicks / GAME_LENGTH);
            zoneChangeBar.setTitle(formatZoneTimeRemaining());
            zoneChangeBar.setProgress((double) (remainingTicks % ZONE_SHRINK_TICKS) / ZONE_SHRINK_TICKS);

            long alivePlayers = getGame().getParticipants().stream()
                    .filter(Game.Participant::isAlive)
                    .count();
            display.text(Component.text("%d Spieler verbleibend".formatted(alivePlayers)));

            forEachPlayerInGame(player -> {
                Location loc = player.getLocation().clone();
                loc.setY(loc.getWorld().getMaxHeight() - 2);
                PacketTricks.sendTeleportPacket(player, display, loc);
                PacketTricks.sendEntityMetaPacket(player, display);
            });
        }

        if (remainingTicks <= 0) {
            finishType = FinishType.NO_WINNER;
            return;
        }

        if (DEBUG_NO_FINISH) {
            return;
        }
        Set<Player> alivePlayers = getAlivePlayers();
        if (alivePlayers.size() == 1) {
            winner = alivePlayers.iterator().next();
            finishType = FinishType.WINNER;
        } else if (alivePlayers.isEmpty()) {
            finishType = FinishType.NO_PLAYERS;
        }
    }

    @Override
    public void onStart() {
        World world = getGame().getWorld();
        remainingTicks = GAME_LENGTH;
        borderShrinkAmount = world.getWorldBorder().getSize() / ZONE_SHRINK_AMOUNT;

        gameEndBar = getPlugin().getServer().createBossBar(formatGameTimeRemaining(), BarColor.WHITE, BarStyle.SEGMENTED_20);
        zoneChangeBar = getPlugin().getServer().createBossBar(formatGameTimeRemaining(), BarColor.RED, BarStyle.SOLID);

        Location displayLoc = getGame().getWorld().getSpawnLocation().clone();
        displayLoc.setY(displayLoc.getWorld().getMaxHeight() - 2);
        display = getGame().getWorld().spawn(displayLoc, TextDisplay.class, display -> {
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBillboard(Display.Billboard.CENTER);
            display.setTransformationMatrix(MatrixMath.combine(
                    MatrixMath.scale(128, 128, 128)
            ));
            display.setViewRange(512);
            display.setDefaultBackground(false);
            display.setTeleportDuration(4);
        });

        getGame().generateSpawnBox(Material.AIR, Material.AIR);
        forEachPlayerInGame(player -> {
            getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> player.setGliding(true), 3);
            player.getEquipment().setChestplate(buildElytra());
            player.setGameMode(GameMode.SURVIVAL);
            player.playSound(player.getLocation(), Sound.ENTITY_GOAT_SCREAMING_PREPARE_RAM, 2, 1);
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.setArrowsInBody(0);

            ignoreFallDamage.add(player.getUniqueId());
            gameEndBar.addPlayer(player);
            zoneChangeBar.addPlayer(player);
        });

        MapView mapView = getGame().getMapView();
        mapView.addRenderer(new GameBorderRenderer(getGame()));
        mapView.addRenderer(new PlayerCursorRenderer(getGame()));

        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, true);

        packetHandlerDestroy = PacketInjector.registerOutboundHandler(ClientboundRemoveEntitiesPacket.class, (player, packet) -> {
            for (Integer entityId : packet.getEntityIds()) {
                if (entityId == display.getEntityId()) {
                    return false;
                }
            }
            return true;
        });
        packetHandlerBlock = PacketInjector.registerOutboundHandler(ClientboundBlockEventPacket.class, (player, packet) -> {
            if (packet.getB0() != 1) {
                return true;
            }
            BlockPos pos = packet.getPos();
            long blockKey = Block.getBlockKey(pos.getX(), pos.getY(), pos.getZ());
            if (!openContainers.contains(blockKey)) {
                return true;
            }
            return packet.getB1() >= 1;

        });

        Chunk spawnChunk = getGame().getWorld().getSpawnLocation().getChunk();
        int half = Game.GAME_WIDTH / 2;
        for (int cx = -half; cx < half; cx++) {
            for (int cz = -half; cz < half; cz++) {
                Chunk chunk = spawnChunk.getWorld().getChunkAt(
                        spawnChunk.getX() + cx,
                        spawnChunk.getZ() + cz
                );
                spawnChest(chunk);
            }
        }
    }

    private void spawnChest(Chunk chunk) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        if (chunk == chunk.getWorld().getSpawnLocation().getChunk()
            || rand.nextInt(32) == 0) {
            spawnHighValueChest(chunk);
            return;
        }
        if (rand.nextInt(8) != 0) {
            return;
        }

        int bx = rand.nextInt(16);
        int bz = rand.nextInt(16);
        Block base = chunk.getWorld().getHighestBlockAt(chunk.getX() * 16 + bx, chunk.getZ() * 16 + bz);
        LootTables table = POSSIBLE_LOOT_TABLES.get(rand.nextInt(POSSIBLE_LOOT_TABLES.size()));
        fillLoot(rand, base, getHolder(base), table);
    }

    private void spawnHighValueChest(Chunk chunk) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        Block base = chunk.getWorld().getHighestBlockAt(chunk.getX() * 16 + 8, chunk.getZ() * 16 + 8);
        InventoryHolder holder = getHolder(base);
        if (holder instanceof Container container) {
            container.getBlock().setType(Material.SHULKER_BOX);
            holder = (InventoryHolder) container.getBlock().getState();
        } else if (holder instanceof ChestBoat boat) {
            boat.setBoatType(Boat.Type.MANGROVE);
        }
        LootTables table = TREASURE_LOOT_TABLES.get(rand.nextInt(TREASURE_LOOT_TABLES.size()));
        fillLoot(rand, base, holder, table);
    }

    private void fillLoot(ThreadLocalRandom rand, Block base, InventoryHolder holder, LootTables table) {
        table.getLootTable().fillInventory(holder.getInventory(), rand, new LootContext.Builder(base.getLocation()).build());
    }

    private InventoryHolder getHolder(Block base) {
        InventoryHolder holder;
        if (base.getType() != Material.WATER) {
            Block block = base.getRelative(BlockFace.UP);
            block.setType(Material.CHEST);
            holder = (Container) block.getState();
        } else {
            holder = base.getWorld().spawn(base.getRelative(BlockFace.UP).getLocation(), ChestBoat.class);
        }
        return holder;
    }

    private ItemStack buildElytra() {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        elytra.editMeta(Damageable.class, d -> d.setDamage(Material.ELYTRA.getMaxDurability() - ELYTRA_DURABILITY));
        return elytra;
    }

    @Override
    public void onEnd() {
        gameEndBar.removeAll();
        zoneChangeBar.removeAll();
        PacketInjector.deregisterOutboundHandler(ClientboundRemoveEntitiesPacket.class, packetHandlerDestroy);
        PacketInjector.deregisterOutboundHandler(ClientboundBlockEventPacket.class, packetHandlerBlock);
        forEachPlayerInGame(p -> PacketTricks.sendEntityRemovePacket(p, display));
        display.remove();
    }

    private String formatGameTimeRemaining() {
        int mins = (remainingTicks / 20) / 60;
        int secs = (remainingTicks / 20) % 60;
        return "Spielende: " + mins + "m " + secs + "s";
    }

    private String formatZoneTimeRemaining() {
        int remainingTicks = this.remainingTicks % ZONE_SHRINK_TICKS;
        int mins = (remainingTicks / 20) / 60;
        int secs = (remainingTicks / 20) % 60;
        return "Zone: " + mins + "m " + secs + "s";
    }

    @EventHandler
    public void onLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isGliding()) {
            recentGlidingPlayers.put(player.getUniqueId(), OBJECT);
        }
        if (player.isGliding()
            || player.getY() >= player.getWorld().getMaxHeight() - 8
            || player.getFallDistance() > 0) {
            return;
        }
        ItemStack chestplate = player.getEquipment().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) {
            return;
        }
        ignoreFallDamage.remove(player.getUniqueId());
        player.getEquipment().setChestplate(null);
    }

    @EventHandler
    public void onInventoryInteract(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) {
            return;
        }
        if (event.getCurrentItem().getType() != Material.ELYTRA) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.ELYTRA) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Game.Participant participant = getGame().getParticipant(player);
        if (participant == null) {
            return;
        }

        Component deathMsg = event.deathMessage();
        player.showTitle(Title.title(
                Component.text("Platz ").color(NamedTextColor.YELLOW)
                        .append(Component.text("#" + participant.getPlacement()).color(NamedTextColor.GOLD)),
                deathMsg == null ? Component.empty() : deathMsg.color(NamedTextColor.GRAY)
        ));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        EntityEquipment equipment = player.getEquipment();
        if (!player.isGliding()
            && recentGlidingPlayers.getIfPresent(player.getUniqueId()) == null
            && (equipment.getChestplate() == null || equipment.getChestplate().getType() != Material.ELYTRA)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        EntityEquipment equipment = player.getEquipment();
        if (equipment.getChestplate() != null && equipment.getChestplate().getType() == Material.ELYTRA) {
            return;
        }
        if (!ignoreFallDamage.remove(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Lidded lidded)) {
            return;
        }
        Block block = ((BlockState) lidded).getBlock();
        openContainers.add(block.getBlockKey());
        forEachPlayerInGame(player -> PacketTricks.sendContainerLidPacket(player, block, true));
    }

    @EventHandler
    public void onChunkSend(PlayerChunkLoadEvent event) {
        for (BlockState tileEntity : event.getChunk().getTileEntities()) {
            if (!(openContainers.contains(tileEntity.getBlock().getBlockKey()))) {
                continue;
            }
            PacketTricks.sendContainerLidPacket(event.getPlayer(), tileEntity.getBlock(), true);
        }
    }

    @Override
    public boolean shouldContinue() {
        return remainingTicks > 0 && finishType == null;
    }

    @Override
    public GameState getNextState() {
        return switch (finishType) {
            case WINNER, NO_WINNER -> new FinishState(winner);
            case NO_PLAYERS -> null;
        };
    }

    private enum FinishType {
        WINNER,
        NO_WINNER,
        NO_PLAYERS
    }
}
