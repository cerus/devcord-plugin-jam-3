package club.devcord.gamejam;


import de.chojo.pluginjam.PluginJam;
import de.chojo.pluginjam.serverapi.ServerApi;
import org.bukkit.plugin.java.JavaPlugin;

public class JamPlugin extends JavaPlugin {

    @Override
    public void onDisable() {

    }

    @Override
    public void onEnable() {
        ServerApi api = getPlugin(PluginJam.class).api();
        // api.requestRestart();
    }
}
