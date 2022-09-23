package dev.tonysp.koth.game;

public enum GameParameter {

    REGION_CENTER ("Region Center"),
    REGION_RADIUS ("Region Radius"),
    CAPTURE_TIME ("Capture Time"),
    REWARD ("Reward"),
    ;

    private final String name;
    GameParameter (String name) {
        this.name = name;
    }

    public String getName () {
        return name;
    }
}
