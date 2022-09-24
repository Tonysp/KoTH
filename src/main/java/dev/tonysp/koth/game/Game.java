package dev.tonysp.koth.game;

import dev.tonysp.koth.KoTH;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Game {

    private final String name;
    private GameState gameState;

    private final Set<GameParameter> missingParameters;

    protected Game (String name, GameState gameState) {
        this.name = name;
        this.gameState = gameState;
        this.missingParameters = new HashSet<>();
        this.missingParameters.addAll(Arrays.asList(GameParameter.values()));
    }

    public abstract void tick ();

    public abstract void save ();

    public abstract void finishGame ();

    public void parameterSet (GameParameter gameParameter) {
        this.missingParameters.remove(gameParameter);
    }

    public String getMissingParametersMessage () {
        return ChatColor.RED + "Missing: " + missingParameters.stream()
                .map(GameParameter::getName)
                .collect(Collectors.joining(", "));
    }

    public boolean isMissingParameters () {
        return !missingParameters.isEmpty();
    }

    public boolean isMissingParameter (GameParameter gameParameter) {
        return missingParameters.contains(gameParameter);
    }

    public String getName () {
        return name;
    }

    public void setGameState (GameState gameState) {
        this.gameState = gameState;
    }

    public boolean isInProgress () {
        return gameState == GameState.IN_PROGRESS;
    }

    public GameState getGameState () {
        return gameState;
    }
}
