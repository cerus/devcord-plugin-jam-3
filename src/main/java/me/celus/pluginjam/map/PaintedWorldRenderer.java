package me.celus.pluginjam.map;

import java.awt.Color;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;
import org.jetbrains.annotations.NotNull;

public class PaintedWorldRenderer extends MapRenderer {

    private final byte[] paintedWorld;
    private boolean rendered;
    private int state;
    private int delay;

    public PaintedWorldRenderer(byte[] paintedWorld) {
        this.paintedWorld = paintedWorld;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (paintedWorld[0] == 0 || paintedWorld[paintedWorld.length - 1] == 0) {
            if (delay-- > 0) {
                return;
            }
            renderWaitingAnimation(map, canvas, player);
            delay = 4;
            return;
        }
        if (rendered) {
            return;
        }

        for (int px = 0; px < 128; px++) {
            for (int py = 0; py < 128; py++) {
                canvas.setPixel(px, py, paintedWorld[px * 128 + py]);
            }
        }
        rendered = true;
    }

    private void renderWaitingAnimation(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (state == 0) {
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    canvas.setPixelColor(x, y, Color.DARK_GRAY);
                }
            }

            String text = "§32;Warte...";
            int textWidth = MinecraftFont.Font.getWidth(text);
            canvas.drawText(128 / 2 - textWidth / 2, 20, MinecraftFont.Font, text);
        }
        if (++state > 4) {
            state = 1;
        }

        if (state == 4) {
            drawDot(canvas, calcX(3), Color.DARK_GRAY);
            drawDot(canvas, calcX(2), Color.DARK_GRAY);
            drawDot(canvas, calcX(1), Color.DARK_GRAY);
        } else {
            drawDot(canvas, calcX(state), Color.WHITE);
        }
    }

    private int calcX(int state) {
        return 64 + (32 * (state - 2));
    }

    private void drawDot(MapCanvas canvas, int x, Color color) {
        for (int xx = -4; xx < 8; xx++) {
            for (int yy = -4; yy < 8; yy++) {
                canvas.setPixelColor(x + xx, 100 + yy, color);
            }
        }
    }
}
