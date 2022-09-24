package dev.tonysp.koth.game;

import dev.tonysp.koth.KoTH;
import dev.tonysp.koth.events.KothCapEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SimpleGame extends Game {

    private Location regionCenter;
    private int regionRadius, captureTime;

    private Player leader;
    private int remainingTicks;

    private List<ItemStack> reward = new ArrayList<>();

    public SimpleGame (String name, GameState gameState) {
        super(name, gameState);
    }

    public void setRegionCenter (Location regionCenter) {
        this.regionCenter = regionCenter;
        parameterSet(GameParameter.REGION_CENTER);
    }

    public void setRegionRadius (int regionRadius) {
        this.regionRadius = regionRadius;
        parameterSet(GameParameter.REGION_RADIUS);
    }

    public void setCaptureTime (int captureTime) {
        this.captureTime = captureTime;
        parameterSet(GameParameter.CAPTURE_TIME);
        resetRemainingTicks();
    }

    public void setReward (List<ItemStack> reward) {
        this.reward = reward;
        parameterSet(GameParameter.REWARD);
    }

    public List<ItemStack> getReward () {
        return reward;
    }

    public void resetRemainingTicks () {
        remainingTicks = captureTime * 4;
    }

    public Location getRegionCenter () {
        return regionCenter;
    }

    public int getRegionRadius () {
        return regionRadius;
    }

    public int getCaptureTime () {
        return captureTime;
    }

    @Override
    public void tick () {
        Optional<Player> controllingPlayer = getControllingPlayer();
        if (!controllingPlayer.isPresent()) {
            resetRemainingTicks();
            return;
        }

        if (leader == null) {
            leader = controllingPlayer.get();
        }

        if (leader.equals(controllingPlayer.get())) {
            remainingTicks --;
        } else {
            resetRemainingTicks();
            leader = controllingPlayer.get();
        }

        if (remainingTicks <= 0) {
            finishGame();
        } else if (remainingTicks % 4 == 0) {
            leader.sendMessage(ChatColor.GREEN + "Capturing in " + (remainingTicks / 4) + " seconds!");
            leader.playSound(leader.getLocation(), Sound.CLICK, 1, 1);
        }
    }

    @Override
    public void finishGame () {
        Bukkit.getPluginManager().callEvent(new KothCapEvent(getName(), leader));
        setGameState(GameState.FINISHED);
        Bukkit.broadcastMessage(ChatColor.GREEN + leader.getName() + " is The King of The Hill!");
        leader.playSound(leader.getLocation(), Sound.ORB_PICKUP, 1, 1);
        if (leader.isOnline()) {
            for (ItemStack item : getReward()) {
                leader.getInventory().addItem(item).values().forEach(overflowItem -> {
                    leader.getLocation().getWorld().dropItemNaturally(leader.getLocation(), overflowItem);
                });
            }
        }
        resetRemainingTicks();
    }

    // Returns a player if he is the only one in the capture area
    private Optional<Player> getControllingPlayer () {
        Optional<Player> controllingPlayer = Optional.empty();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().getWorld().equals(regionCenter.getWorld())
                    && player.getLocation().distance(regionCenter) <= regionRadius) {
                if (controllingPlayer.isPresent()) {
                    return Optional.empty();
                } else {
                    controllingPlayer = Optional.of(player);
                }
            }
        }

        return controllingPlayer;
    }

    @Override
    public void save () {
        FileConfiguration config = KoTH.getInstance().getConfig();

        String key = "games." + getName();
        config.set(key + ".game-state", getGameState().toString());
        if (!isMissingParameter(GameParameter.REGION_CENTER)) {
            config.set(key + ".region-center.world", regionCenter.getWorld().getName());
            config.set(key + ".region-center.x", regionCenter.getBlockX());
            config.set(key + ".region-center.y", regionCenter.getBlockY());
            config.set(key + ".region-center.z", regionCenter.getBlockZ());
        }

        if (!isMissingParameter(GameParameter.REGION_RADIUS)) {
            config.set(key + ".region-radius", regionRadius);
        }

        if (!isMissingParameter(GameParameter.CAPTURE_TIME)) {
            config.set(key + ".capture-time", captureTime);
        }

        if (!isMissingParameter(GameParameter.REWARD)) {
            config.set(key + ".reward", reward);
        }
    }
}
