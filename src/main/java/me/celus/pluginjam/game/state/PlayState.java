package me.celus.pluginjam.game.state;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.celus.pluginjam.game.GameState;
import me.celus.pluginjam.map.GameBorderRenderer;
import me.celus.pluginjam.map.PlayerCursorRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.map.MapView;

public class PlayState extends GameState {

    private static final boolean DEBUG_NO_FINISH = true;

    private static final int GAME_LENGTH = 20 * 60 * 10;
    private static final int ZONE_SHRINK_AMOUNT = 4;
    private static final int ZONE_SHRINK_TICKS = GAME_LENGTH / ZONE_SHRINK_AMOUNT;
    private static final int ELYTRA_DURABILITY = 30;
    private static final Object OBJECT = new Object();

    private final Cache<UUID, Object> recentGlidingPlayers = CacheBuilder.newBuilder().expireAfterWrite(500, TimeUnit.MILLISECONDS).build();
    private final Set<UUID> ignoreFallDamage = new HashSet<>();
    private BossBar gameEndBar;
    private BossBar zoneChangeBar;
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
            broadcast(Component.text("DIE ZONE SCHRUMPFT").color(NamedTextColor.RED));
            forEachPlayerInGame(player -> player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.25f, 1));
        }
        if (remainingTicks % 5 == 0) {
            gameEndBar.setTitle(formatGameTimeRemaining());
            gameEndBar.setProgress((double) remainingTicks / GAME_LENGTH);
            zoneChangeBar.setTitle(formatZoneTimeRemaining());
            zoneChangeBar.setProgress((double) (remainingTicks % ZONE_SHRINK_TICKS) / ZONE_SHRINK_TICKS);
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
        remainingTicks = GAME_LENGTH;
        borderShrinkAmount = getGame().getWorld().getWorldBorder().getSize() / ZONE_SHRINK_AMOUNT;

        gameEndBar = getPlugin().getServer().createBossBar(formatGameTimeRemaining(), BarColor.WHITE, BarStyle.SEGMENTED_20);
        zoneChangeBar = getPlugin().getServer().createBossBar(formatGameTimeRemaining(), BarColor.RED, BarStyle.SOLID);

        getGame().generateSpawnBox(Material.AIR, Material.AIR);
        forEachPlayerInGame(player -> {
            getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> player.setGliding(true), 3);
            player.getEquipment().setChestplate(buildElytra());
            player.setGameMode(GameMode.SURVIVAL);
            player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 1);
            ignoreFallDamage.add(player.getUniqueId());
            gameEndBar.addPlayer(player);
            zoneChangeBar.addPlayer(player);
        });

        MapView mapView = getGame().getMapView();
        mapView.addRenderer(new GameBorderRenderer(getGame()));
        mapView.addRenderer(new PlayerCursorRenderer(getGame()));
    }

    private ItemStack buildElytra() {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        elytra.editMeta(Damageable.class, d -> d.setDamage(Material.ELYTRA.getMaxDurability() - ELYTRA_DURABILITY));
        return elytra;
    }

    @Override
    public void onEnd() {
        gameEndBar.removeAll();
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
    public void onDamage2(EntityDamageEvent event) {
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
