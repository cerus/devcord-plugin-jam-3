package me.celus.pluginjam.game.state;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import me.celus.pluginjam.game.GameState;
import me.celus.pluginjam.util.MatrixMath;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

public class WaitingState extends GameState {

    private static final int MIN_PLAYERS = 4;
    private static final int START_DELAY = 20 * 20 + 1;
    private static final int DISPLAY_REMOVE_DELAY = 40;
    private final Set<TextDisplay> explanationDisplays = new HashSet<>();
    private TextDisplay display;
    private int delay;
    private boolean forceStart;

    @Override
    public void tick(int totalElapsedTicks) {
        World gameWorld = getGame().getWorld();
        int playerCount = gameWorld.getPlayerCount();
        if (playerCount < MIN_PLAYERS && !forceStart) {
            delay = START_DELAY;
            display.text(Component.text("Warte... (%d/%d)".formatted(playerCount, MIN_PLAYERS)));
            return;
        } else {
            delay--;
        }

        if (delay % 20 == 0) {
            int secondsLeft = delay / 20;
            if (secondsLeft > 0 && (secondsLeft <= 5 || secondsLeft % 10 == 0)) {
                gameWorld.getPlayers().forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1));
            }
        }
        if (totalElapsedTicks % 5 == 0) {
            display.text(Component.text("%d Sekunden".formatted(delay / 20)));
            gameWorld.getPlayers().forEach(player -> player.sendActionBar(Component.text("Spiel startet in %d Sekunden".formatted(delay / 20))));
        }
    }

    @Override
    public void onStart() {
        delay = START_DELAY;
        Location displayLoc = getGame().getWorld().getSpawnLocation().clone().subtract(0, 64, 0);
        display = getGame().getWorld().spawn(displayLoc, TextDisplay.class, display -> {
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformationMatrix(MatrixMath.combine(
                    MatrixMath.combineAndExpand(
                            MatrixMath.rotationX((float) Math.toRadians(-90)),
                            MatrixMath.rotationZ((float) Math.toRadians(180))
                    ),
                    MatrixMath.scale(64, 64, 64)
            ));
            display.setDefaultBackground(false);
            display.setInterpolationDuration(DISPLAY_REMOVE_DELAY);
        });
        forEachPlayerInGame(this::setupPlayer);

        explanation(0, 1.5, 7, """
                <gradient:#7a0000:#ad1818:#7a0000><bold>Battle Royale</bold></gradient><reset>
                Willkommen bei Battle Royale! Du trittst
                mit mehreren Spielern gegeneinander
                auf einer zufällig generierten Karte
                an, mit dem Ziel, am Ende die letzte
                lebende Person zu sein. Aber Achtung:
                diese Welt ist alles andere als normal.
                Viel Glück!
                """);
        explanation(4, 1.5, 7, """
                <#6b9dc2><bold>Commands:</bold></#6b9dc2>
                <#9ac0db>/startgame
                /setdelay</#9ac0db>
                """);
        explanation(-5, 1.5, 7, """
                <#cf9340><bold>Checkliste:</bold></#cf9340>
                <#cfaa76>Mobs anschauen
                Schafe scheren
                Schwimmen gehen
                Portale verwenden
                Pfeil und Bogen verwenden
                Enderman sauer machen</#cfaa76>
                """);
        explanation(1, 0.5, 7, d -> {
            d.text(getPlugin().getDeveloperMax());
            d.setTransformationMatrix(MatrixMath.combine(
                    MatrixMath.combineAndExpand(
                            MatrixMath.rotationY((float) Math.toRadians(180)),
                            MatrixMath.rotationZ((float) Math.toRadians(10))
                    ),
                    MatrixMath.scale(0.15f, 0.15f, 0.15f))
            );
        });
        explanation(-1, 0.5, 7, d -> {
            d.text(getPlugin().getDeveloperLukas());
            d.setTransformationMatrix(MatrixMath.combine(
                    MatrixMath.combineAndExpand(
                            MatrixMath.rotationY((float) Math.toRadians(180)),
                            MatrixMath.rotationZ((float) Math.toRadians(-10))
                    ),
                    MatrixMath.scale(0.15f, 0.15f, 0.15f))
            );
        });
    }

    @Override
    public void onEnd() {
        display.text(Component.text("GLHF!"));
        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
            display.setInterpolationDelay(-1);
            display.setTransformationMatrix(MatrixMath.combine(
                    MatrixMath.combineAndExpand(
                            MatrixMath.rotationX((float) Math.toRadians(-90)),
                            MatrixMath.rotationZ((float) Math.toRadians(180))
                    ),
                    MatrixMath.scale(0.1f, 0.1f, 0.1f)
            ));
        }, 2);
        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> display.remove(), DISPLAY_REMOVE_DELAY);
        explanationDisplays.forEach(Entity::remove);
    }

    private void explanation(double x, double y, double z, String text) {
        explanation(x, y, z, d -> d.text(MiniMessage.miniMessage().deserialize(text.trim())));
    }

    private void explanation(double x, double y, double z, Consumer<TextDisplay> modifier) {
        Location displayLoc = getGame().getWorld().getSpawnLocation().clone().add(x, y, z);
        TextDisplay textDisplay = getGame().getWorld().spawn(displayLoc, TextDisplay.class, display -> {
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformationMatrix(MatrixMath.combineAndExpand(
                    MatrixMath.rotationY((float) Math.toRadians(180))
            ));
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setShadowed(false);
            modifier.accept(display);
        });
        explanationDisplays.add(textDisplay);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        setupPlayer(player);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        event.setCancelled(true);
    }

    private void setupPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setExp(0);
        player.setLevel(0);
        player.teleport(getGame().getWorld().getSpawnLocation());
        player.setGlowing(false);
        player.clearActivePotionEffects();
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        player.setFoodLevel(20);
        player.setSaturation(20);

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        mapItem.editMeta(MapMeta.class, mapMeta -> mapMeta.setMapView(getGame().getMapView()));
        player.getInventory().setItemInOffHand(mapItem);
    }

    @Override
    public boolean shouldContinue() {
        return delay > 0;
    }

    @Override
    public GameState getNextState() {
        return new PlayState();
    }

    public void forceStart() {
        delay = Math.min(delay, 5 * 20 + 1);
        forceStart = true;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}
