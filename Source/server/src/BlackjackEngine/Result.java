package BlackjackEngine;

public enum Result {
    
	NONE("None"),
	DEALER_LOSE("Dealer Lose"),
	PLYAER_LOSE("Player Lose"),
	DRAW("Draw"),
	FOLD("Fold"),
	BUSTED("Busted"),
	NEW("New"),

    ;
    
    /** Display name. */
    private String name;
    
    /**
     * Constructor.
     * 
     * @param name
     *            The display name.
     */
    Result(String name) {
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
