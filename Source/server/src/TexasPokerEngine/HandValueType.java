package TexasPokerEngine;

public enum HandValueType {
    
    /** Royal flush (Ace-high Straight Flush). */
    ROYAL_FLUSH("Royal flush", 9),
    
    /** Straight Flush (a Straight and a Flush, less than Ace-high). */
    STRAIGHT_FLUSH("Straight flush", 8),

    /** Four of a Kind (four cards of the same rank). */ 
    FOUR_OF_A_KIND("Four of a kind", 7),
    
    /** Full House (a Three of a Kind and Two Pairs). */
    FULL_HOUSE("Full house", 6),
    
    /** Flush (five cards of the same suit). */
    FLUSH("Flush", 5),
    
    /** Straight (five cards in sequential order). */
    STRAIGHT("Straight", 4),

    /** Three of a Kind (three cards of the same rank). */
    THREE_OF_A_KIND("Three of a kind", 3),
    
    /** Two Pairs (two pairs). */
    TWO_PAIRS("Two pair", 2),

    /** One Pair (two cards of the same rank). */
    ONE_PAIR("Pair", 1),
    
    /** Highest Card (the card with the highest rank). */
    HIGH_CARD("High card", 0),

    ;
    
    /** The description. */
    private String description;

    /** The hand value. */
    private int value;
    
    /**
     * Constructor.
     * 
     * @param description
     *            The description.
     * @param value
     *            The hand value.
     */
    HandValueType(String description, int value) {
        this.description = description;
        this.value = value;
    }
    
    /**
     * Returns the description.
     * 
     * @return The description.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the hand value.
     * 
     * @return The hand value.
     */
    public int getValue() {
        return value;
    }
    
}
