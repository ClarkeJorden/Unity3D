package RouletteEngine;

public enum Status {
    
	WAITING("Waiting", 0),
	BET("Bet", 1),
    SPIN("Spin", 2),
    PAYOUT("Payout", 3)

    ;
    
    /** Display name. */
    private String name;
    private int value;
    
    /**
     * Constructor.
     * 
     * @param name
     *            The display name.
     */
    Status(String name, int value) {
        this.name = name;
        this.value = value;
    }
    
    /**
     * Returns the display name.
     * 
     * @return The display name.
     */
    public String getName() {
        return name;
    }
    
    public int getValue() {
    	return value;
    }

}
