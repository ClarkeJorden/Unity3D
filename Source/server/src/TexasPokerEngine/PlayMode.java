package TexasPokerEngine;

public enum PlayMode {
    
    NORMAL_MODE("Normal_Mode"),    
    TOURNAMENT_MODE("Tournament_Mode"),
    CLUB_MODE("Club_Mode"),
    
    ;
    
    /** Display name. */
    private String name;
    
    /**
     * Constructor.
     * 
     * @param name
     *            The display name.
     */
    PlayMode(String name) {
        this.name = name;
    }
    
    /**
     * Returns the display name.
     * 
     * @return The display name.
     */
    public String getName() {
        return name;
    }

}
