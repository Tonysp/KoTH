package dev.tonysp.koth;

import dev.tonysp.koth.commands.KoTHCommand;
import dev.tonysp.koth.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class KoTH extends JavaPlugin {

    public static KoTH getInstance () {
        return getPlugin(KoTH.class);
    }

    private final GameManager gameManager = new GameManager(this);

    private File configFile;

    @Override
    public void onEnable () {
        log(enable());
    }

    @Override
    public void onDisable () {
        log(disable().join());
    }

    public String enable () {
        loadConfig();
        String failed = ChatColor.RED + "Plugin failed to enable, check console!";

        getCommand("koth").setExecutor(new KoTHCommand(this));

        if (!getGames().load()) {
            return failed;
        }

        return ChatColor.GREEN + "Plugin enabled!";
    }

    public CompletableFuture<String> disable () {
        return CompletableFuture.supplyAsync(() -> {
            getGames().unloadAsync().join();
            return ChatColor.GREEN + "Plugin disabled!";
        });
    }

    private void loadConfig () {
        configFile = new File(getDataFolder() + File.separator + "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        try {
            YamlConfiguration yamlConfig = new YamlConfiguration();
            yamlConfig.load(new File(getDataFolder() + File.separator + "config.yml"));
        } catch (Exception exception) {
            log("There was a problem loading the config. More details bellow.");
            exception.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        reloadConfig();
    }

    public GameManager getGames () {
        return gameManager;
    }

    public static void logWarning (String text) {
        log(Level.INFO, "! ! ! " + text + " ! ! !");
    }

    public static void log (Level level, String text) {
        Bukkit.getLogger().log(level, "[KoTH] " + text);
    }

    public static void log (String text) {
        log(Level.INFO, text);
    }

    public File getConfigFile () {
        return configFile;
    }
}
