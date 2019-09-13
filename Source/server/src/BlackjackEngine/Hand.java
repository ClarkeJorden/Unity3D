package BlackjackEngine;

import java.util.Collection;

public class Hand {
    
    private static final int MAX_NO_OF_CARDS = 22;
    private Card[] cards = new Card[MAX_NO_OF_CARDS];
    private int noOfCards = 0;
    
    public Hand() {
    }
    
    public Hand(Card[] cards) {
        addCards(cards);
    }
    
    public Hand(Collection<Card> cards) {
        if (cards == null) {
            throw new IllegalArgumentException("Null array");
        }
        for (Card card : cards) {
            addCard(card);
        }
    }
    
    /**
     * Constructor with a string representing the initial cards.
     * 
     * The string must contain of one or more cards.
     * A card must be represented by a rank and a suit character.
     * The cards must be separated by a space character.
     * 
     * Example: "Kh 7d 4c As Js"
     * 
     * @param s
     *            The string to parse.
     * 
     * @throws IllegalArgumentException
     *             If the string could not be parsed or the number of cards is
     *             too high.
     */
    public Hand(String s) {
        if (s == null || s.length() == 0) {
            throw new IllegalArgumentException("Null or empty string");
        }
        
        String[] parts = s.split("\\s");
        if (parts.length > MAX_NO_OF_CARDS) {
            throw new IllegalArgumentException("Too many cards in hand");
        }
        for (String part : parts) {
            addCard(new Card(part));
        }
    }
    
    /**
     * Returns the number of cards.
     * 
     * @return The number of cards.
     */
    public int size() {
        return noOfCards;
    }
    
    /**
     * Adds a single card.
     * 
     * The card is inserted at such a position that the hand remains sorted
     * (highest ranking cards first).
     * 
     * @param card
     *            The card to add.
     * 
     * @throws IllegalArgumentException
     *             If the card is null.
     */
    public void addCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Null card");
        }
        
        int insertIndex = -1;
        for (int i = 0; i < noOfCards; i++) {
            if (card.compareTo(cards[i]) > 0) {
                insertIndex = i;
                break;
            }
        }
        if (insertIndex == -1) {
            // Could not insert anywhere, so append at the end.
            cards[noOfCards++] = card;
        } else {
            System.arraycopy(cards, insertIndex, cards, insertIndex + 1, noOfCards - insertIndex);
            cards[insertIndex] = card;
            noOfCards++;
        }
    }
    
    /**
     * Adds multiple cards.
     * 
     * The cards are inserted at such a position that the hand remains sorted
     * (highest ranking cards first).
     * 
     * @param cards
     *            The cards to add.
     */
    public void addCards(Card[] cards) {
        if (cards == null) {
            throw new IllegalArgumentException("Null array");
        }
        if (cards.length > MAX_NO_OF_CARDS) {
            throw new IllegalArgumentException("Too many cards");
        }
        for (Card card : cards) {
            addCard(card);
        }
    }
    
    /**
     * Adds multiple cards.
     * 
     * The cards are inserted at such a position that the hand remains sorted
     * (highest ranking cards first).
     * 
     * @param cards
     *            The cards to add.
     */
    public void addCards(Collection<Card> cards) {
        if (cards == null) {
            throw new IllegalArgumentException("Null collection");
        }
        if (cards.size() > MAX_NO_OF_CARDS) {
            throw new IllegalArgumentException("Too many cards");
        }
        for (Card card : cards) {
            addCard(card);
        }
    }
    
    /**
     * Returns the cards.
     *
     * @return The cards.
     */
    public Card[] getCards() {
        Card[] dest = new Card[noOfCards];
        System.arraycopy(cards, 0, dest, 0, noOfCards);
        return dest;
    }
    
    /**
     * Removes all cards.
     */
    public void removeAllCards() {
        noOfCards = 0;
    }
    
    public void removeLastCard() {
    	noOfCards --;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < noOfCards; i++) {
            sb.append(cards[i]);
            if (i < (noOfCards - 1)) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
    
    public int calculateHand()
    {
        int sum = 0;
        boolean haventSeenA = true;
        for(Card card : cards)
        {
            if (haventSeenA && card.getRank() == Card.ACE)
            {
                sum += 11;
                haventSeenA = false;
            } else if (card.getRank() >= Card.JACK)
            {
                sum += 10;
            } else
            {
                sum += card.getRank() + 2;
            }
        }
        if (sum > 21 && !haventSeenA)
        {
            sum -= 10;
        }
        return sum;
    }
    
    public boolean isBlackjack()
    {
    	return (cards.length == 2 && calculateHand() == 21);
    }
    
    public boolean isSplit()
    {
    	if(cards.length != 2)
    		return false;
    	if(cards[0].getRank() >= Card.JACK && cards[0].getRank() <= Card.KING) {
    		if(cards[1].getRank() >= Card.JACK && cards[1].getRank() <= Card.KING)
    			return true;
    	} else if(cards[0].getRank() == cards[1].getRank())
    		return true;
    	return false;
    }
    
}
