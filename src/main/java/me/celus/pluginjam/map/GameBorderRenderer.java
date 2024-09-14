package me.celus.pluginjam.map;

import java.awt.Color;
import me.celus.pluginjam.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

public class GameBorderRenderer extends MapRenderer {

    private final Game game;
    private double borderSize;

    public GameBorderRenderer(Game game) {
        this.game = game;
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (canvas.getBasePixelColor(0, 0).equals(Color.DARK_GRAY)) {
            // Shitty hack
            return;
        }

        double currentBorderSize = map.getWorld().getWorldBorder().getSize();
        if (currentBorderSize == borderSize) {
            return;
        }
        borderSize = currentBorderSize;

        int scaled = (int) ((borderSize / (Game.GAME_WIDTH * 16)) * 128) - 1;
        int mx = (128 - scaled) / 2;
        int my = (128 - scaled) / 2;
        for (int i = 0; i < scaled; i++) {
            canvas.setPixelColor(i + mx, my, Color.RED);
            canvas.setPixelColor(i + mx, my + scaled, Color.RED);
            canvas.setPixelColor(mx, my + i, Color.RED);
            canvas.setPixelColor(mx + scaled, my + i + 1, Color.RED);
        }
    }
}
