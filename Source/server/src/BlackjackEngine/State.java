package BlackjackEngine;

public enum State {
    
    BETTING("Betting"),

    ;
    
    /** Display name. */
    private String name;
    
    /**
     * Constructor.
     * 
     * @param name
     *            The display name.
     */
    State(String name) {
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
