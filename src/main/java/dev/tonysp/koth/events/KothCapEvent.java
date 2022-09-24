package dev.tonysp.koth.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class KothCapEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final String kothName;
    private final Player player;


    public KothCapEvent (String kothName, Player player) {
        this.kothName = kothName;
        this.player = player;
    }

    public String getKothName () {
        return kothName;
    }

    public Player getPlayer () {
        return player;
    }

    @Override
    public HandlerList getHandlers () {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
