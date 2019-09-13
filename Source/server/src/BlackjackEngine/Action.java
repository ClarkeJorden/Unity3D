package BlackjackEngine;

public class Action {

	public static final Action NONE = new Action("None");
	public static final Action BET = new Action("Bet");
	public static final Action FOLD = new Action("Fold");
	public static final Action HIT = new Action("Hit");
	public static final Action STAND = new Action("Stand");
	public static final Action DOUBLE = new Action("Double");
	public static final Action SPLIT = new Action("Split");
    
    /** The action's name. */
    private final String name;
    
    /**
     * Constructor.
     * 
     * @param name
     *            The action's name.
     * @param verb
     *            The action's verb.
     */
    public Action(String name) {
    	this.name = name;
    }
        
    /**
     * Returns the action's name.
     * 
     * @return The action's name.
     */
    public final String getName() {
        return name;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name;
    }

}
