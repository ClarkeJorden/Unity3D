package TexasPokerAction;

import java.math.BigDecimal;

public class Action {
    
    /** Player went all-in. */
    public static final Action ALL_IN = new AllInAction();

    /** Player went all-in. */
    public static final Action BUSTED = new BustedAction();

    /** Bet. */
    public static final Action BET = new BetAction(BigDecimal.ZERO);
    
    /** Call. */
    public static final Action CALL = new CallAction();
    
    /** Check. */
    public static final Action CHECK = new CheckAction();
    
    /** Continue. */
    public static final Action CONTINUE = new ContinueAction();
    
    /** Fold. */
    public static final Action FOLD = new FoldAction();
    
    /** Raise. */
    public static final Action RAISE = new RaiseAction(BigDecimal.ZERO);
    
    public static final Action NO_RESPONSE = new Action("NoResponse", "NoResponse");
    
    public static final Action SIT_OUT = new Action("SitOut", "SitOut");

    public static final Action ANTE = new Action("Ante", "Ante");
    public static final Action SMALL_BLIND = new Action("Small blind", "Small blind");
    public static final Action BIG_BLIND = new Action("Big blind", "Big blind");
    
    public static final Action ROYAL_FLUSH = new Action("Royal Flush", "Royal Flush");
    public static final Action STRAIGHT_FLUSH = new Action("Straight Flush", "Straight Flush");
    public static final Action FOUR_OF_A_KIND = new Action("Four of a Kind", "Four of a Kind");
    public static final Action FULL_HOUSE = new Action("Full House", "Full House");
    public static final Action FLUSH = new Action("Flush", "Flush");
    public static final Action STRAIGHT = new Action("Straight", "Straight");
    public static final Action THREE_OF_A_KIND = new Action("Three of a Kind", "Three of a Kind");
    public static final Action TWO_PAIR = new Action("Two Pairs", "Two Pair");
    public static final Action PAIR = new Action("One Pair", "Pair");
    public static final Action HIGH_CARD = new Action("High Card", "High Card");
    
    /** The action's name. */
    private final String name;
    
    /** The action's verb. */
    private final String verb;
    
    /** The amount (if appropriate). */
    private final BigDecimal amount;

    /**
     * Constructor.
     * 
     * @param name
     *            The action's name.
     * @param verb
     *            The action's verb.
     */
    public Action(String name, String verb) {
        this(name, verb, BigDecimal.ZERO);
    }
    
    /**
     * Constructor.
     * 
     * @param name
     *            The action's name.
     * @param verb
     *            The action's verb.
     * @param amount
     *            The action's amount.
     */
    public Action(String name, String verb, BigDecimal amount) {
        this.name = name;
        this.verb = verb;
        this.amount = amount;
    }
    
    /**
     * Returns the action's name.
     * 
     * @return The action's name.
     */
    public final String getName() {
        return name;
    }
    
    /**
     * Returns the action's verb.
     * 
     * @return The action's verb.
     */
    public final String getVerb() {
        return verb;
    }
    
    /**
     * Returns the action's amount.
     * 
     * @return The action's amount.
     */
    public final BigDecimal getAmount() {
        return amount;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name;
    }

}
