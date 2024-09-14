package me.celus.pluginjam.game.state;

import me.celus.pluginjam.game.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

public class FinishState extends GameState {

    private static final int WAIT_UNTIL_RESET_TICKS = 20 * 10;

    private final Player winner;
    private int remaingTicks;

    public FinishState(Player winner) {
        this.winner = winner;
    }

    @Override
    public void tick(int totalElapsedTicks) {
        remaingTicks--;
    }

    @Override
    public void onStart() {
        remaingTicks = WAIT_UNTIL_RESET_TICKS;
        if (winner != null) {
            broadcastTitle(
                    Component.text(winner.getName()).color(NamedTextColor.YELLOW),
                    Component.text("hat die Runde gewonnen").color(NamedTextColor.GRAY)
            );
        } else {
            broadcastTitle(
                    Component.text("Niemand").color(NamedTextColor.RED),
                    Component.text("hat die Runde gewonnen").color(NamedTextColor.GRAY)
            );
        }
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
