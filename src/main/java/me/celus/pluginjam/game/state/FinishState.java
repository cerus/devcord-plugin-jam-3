package me.celus.pluginjam.game.state;

import java.util.concurrent.ThreadLocalRandom;
import me.celus.pluginjam.game.Game;
import me.celus.pluginjam.game.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.meta.FireworkMeta;

public class FinishState extends GameState {

    private static final int WAIT_UNTIL_RESET_TICKS = 20 * 10;

    private final Player winner;
    private int remaingTicks;

    public FinishState(Player winner) {
        this.winner = winner;
    }

    @Override
    public void tick(int totalElapsedTicks) {
        if (winner != null && remaingTicks % 10 == 0) {
            spawnFireworks();
            spawnParticles();
        }
        remaingTicks--;
    }

    private void spawnParticles() {
        winner.getWorld().spawnParticle(Particle.HEART, winner.getEyeLocation(), 10, 0.5f, 0.5f, 0.5f);
    }

    private void spawnFireworks() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        Location loc = winner.getLocation().clone().add(
                rand.nextInt(10) - 5,
                5,
                rand.nextInt(10) - 5
        );

        winner.getWorld().spawn(loc, Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.setPower(rand.nextInt(3) + 1);
            for (int i = 0; i < rand.nextInt(5); i++) {
                meta.addEffect(randomEffect(rand));
            }
            firework.setFireworkMeta(meta);
            firework.setTicksToDetonate(rand.nextInt(20 * 3));
        });
    }

    private FireworkEffect randomEffect(ThreadLocalRandom rand) {
        return FireworkEffect.builder()
                .flicker(rand.nextBoolean())
                .trail(rand.nextBoolean())
                .with(FireworkEffect.Type.values()[rand.nextInt(FireworkEffect.Type.values().length)])
                .withColor(Color.fromRGB(rand.nextInt(0xFFFFFF)))
                .withFade(Color.fromRGB(rand.nextInt(0xFFFFFF)))
                .build();
    }

    @Override
    public void onStart() {
        remaingTicks = WAIT_UNTIL_RESET_TICKS;
        Component winnerComp;
        if (winner != null) {
            winnerComp = Component.text(winner.getName()).color(NamedTextColor.YELLOW);
            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        } else {
            winnerComp = Component.text("Niemand").color(NamedTextColor.RED);
        }
        broadcastTitle(winnerComp, Component.text("hat die Runde gewonnen").color(NamedTextColor.GRAY));

        forEachPlayerInGame(player -> {
            Game.Participant participant = getGame().getParticipant(player);
            if (participant == null) {
                return;
            }
            int placement = participant.getPlacement();
            if (placement == 0) {
                placement = 1;
            }
            player.sendMessage(Component.empty()
                    .append(winnerComp)
                    .append(Component.text(" hat das Spiel gewonnen. Du hast Platz ").color(NamedTextColor.GRAY))
                    .append(Component.text("#" + placement).color(NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(" erreicht.").color(NamedTextColor.GRAY)));
        });
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        event.setCancelled(true);
    }

    @Override
    public boolean shouldContinue() {
        return remaingTicks > 0;
    }

    @Override
    public GameState getNextState() {
        return null;
    }
}
