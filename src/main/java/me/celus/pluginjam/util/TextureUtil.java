package me.celus.pluginjam.util;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.imageio.ImageIO;
import me.celus.pluginjam.JamPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;

public final class TextureUtil {

    private TextureUtil() {
        throw new UnsupportedOperationException();
    }

    public static CompletableFuture<Component> getHeadComponent(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            BufferedImage skin;
            try {
                skin = ImageIO.read(URI.create("https://minotar.net/helm/%s/16".formatted(playerId)).toURL());
            } catch (IOException e) {
                return Component.empty();
            }

            Component base = Component.empty();
            for (int y = 0; y < skin.getHeight(); y++) {
                if (y > 0) {
                    base = base.appendNewline();
                }
                for (int x = 0; x < skin.getWidth(); x++) {
                    int rgb = skin.getRGB(x, y);
                    base = base.append(Component.text("█").color(TextColor.color(rgb)));
                }
            }
            return base;
        }, getAsyncExecutor());
    }

    private static Executor getAsyncExecutor() {
        return command -> Bukkit.getScheduler().runTaskAsynchronously(JamPlugin.getPlugin(JamPlugin.class), command);
    }
}
