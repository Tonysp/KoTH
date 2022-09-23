package dev.tonysp.koth.game;

import dev.tonysp.koth.KoTH;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GameManager implements Listener {

    private final KoTH plugin;

    private final Map<String, Game> games = new HashMap<>();

    private CompletableFuture<Void> modifyingGamesFuture = new CompletableFuture<>();
    private int gameTickTaskId;
    private BukkitTask saveTask;
    private CompletableFuture<Void> saveFuture;

    public GameManager (KoTH plugin) {
        this.plugin = plugin;
    }

    public boolean load () {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        ConfigurationSection config = plugin.getConfig().getConfigurationSection("games");
        if (config == null) {
            return false;
        }

        for (String gameName : config.getKeys(false)) {
            String failedMessage = "Failed to load the game " + gameName;
            ConfigurationSection gameConfig = config.getConfigurationSection(gameName);
            GameState gameState;
            try {
                gameState = GameState.valueOf(gameConfig.getString("game-state"));
            } catch (Exception ignored) {
                KoTH.logWarning(failedMessage + " - invalid game state");
                continue;
            }

            SimpleGame simpleGame = new SimpleGame(gameName, gameState);
            games.put(gameName, simpleGame);


            World world = Bukkit.getWorld(gameConfig.getString("region-center.world"));
            if (world != null) {
                Location regionCenter = new Location(world, gameConfig.getDouble("region-center.x"), gameConfig.getDouble("region-center.y"), gameConfig.getDouble("region-center.z"));
                simpleGame.setRegionCenter(regionCenter);
            }

            int regionRadius = gameConfig.getInt("region-radius");
            if (regionRadius != 0) {
                simpleGame.setRegionRadius(regionRadius);
            }

            int captureTime = gameConfig.getInt("capture-time");
            if (captureTime != 0) {
                simpleGame.setCaptureTime(captureTime);
            }

            List<ItemStack> reward = (List<ItemStack>) gameConfig.getList("reward");
            simpleGame.setReward(reward);
        }

        getModifyingGamesFuture().complete(null);

        gameTickTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (isSaving()) {
                return;
            }

            startModifyingGames();
            games.values().stream().filter(Game::isInProgress).forEach(Game::tick);
            endModifyingGames();
        }, 5L, 5L);

        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(KoTH.getInstance(), this::scheduleSave, 100, 6000);

        return true;
    }

    public CompletableFuture<Void> scheduleSave () {
        saveFuture = CompletableFuture.runAsync(this::save);
        return saveFuture;
    }

    private void save () {
        for (Game game : games.values()) {
            game.save();
        }
        try {
            plugin.getConfig().save(plugin.getConfigFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isSaving () {
        return saveFuture != null && !saveFuture.isDone();
    }

    public CompletableFuture<Void> unloadAsync () {
        Bukkit.getScheduler().cancelTask(gameTickTaskId);
        saveTask.cancel();
        if (saveFuture.isDone()) {
            return scheduleSave();
        } else {
            return saveFuture;
        }
    }

    public Optional<Game> getGameByName (String name) {
        return Optional.ofNullable(games.get(name));
    }

    public Game create (String name) {
        SimpleGame simpleGame = new SimpleGame(name, GameState.NOT_RUNNING);
        games.put(name, simpleGame);
        return simpleGame;
    }

    public void startGame (Game game) {
        game.setGameState(GameState.IN_PROGRESS);
    }

    public CompletableFuture<Void> getModifyingGamesFuture () {
        return modifyingGamesFuture;
    }

    public void startModifyingGames () {
        endModifyingGames();
        modifyingGamesFuture = new CompletableFuture<>();
    }

    public void endModifyingGames () {
        modifyingGamesFuture.complete(null);
    }
}