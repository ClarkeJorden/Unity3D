package BlackjackEngine;

import java.util.List;

public class Player implements Comparable<Player> {
	
	private int pos;
	private String email;
	private String name;
	private int cash;
	private int bet;
    public Hand hand;
    public Hand hand1;
    private boolean hasCards;
    public Action action;
    public Result result;
    
    public boolean isDealerShow = false;

    public Player(int pos, String email, String name, int cash) {
    	this.pos = pos;
    	this.email = email;
    	this.name = name;
    	this.cash = cash;
    	this.result = Result.NEW;
        hand = new Hand();
        resetHand();
    }
    
    @Override
    public int compareTo(Player compPlayer) {
        /* For Ascending order*/
    	return Integer.compare(this.pos, compPlayer.pos);
    }

    public void resetHand() {
    	bet = 0;
    	isDealerShow = false;
    	action = Action.NONE;
    	if(result != Result.BUSTED || result != Result.NEW)
    		result = Result.NONE;
        hasCards = false;
        hand.removeAllCards();
        hand1.removeAllCards();
    }

    public int getPos() {
        return pos;
    }
    
    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public int getCash() {
        return cash;
    }
    
    public void setCash(int d) {
    	cash = d;
    }

    public void payCash(int amount) {
        if (amount > cash) {
            throw new IllegalStateException("Player asked to pay more cash than he owns!");
        }
        cash -= amount;
    }
    
    public int getBet() {
    	return bet;
    }
    
    public void setBet(int d) {
    	bet = d;
    }

    public void setCards(List<Card> cards) {
        hand.removeAllCards();
        if (cards != null) {
            if (cards.size() == 2) {
                hand.addCards(cards);
                hasCards = true;
                System.out.format("[CHEAT] %s's cards:\t%s\n", name, hand);
            } else {
                throw new IllegalArgumentException("Invalid number of cards");
            }
        }
    }
    
    public Card[] getCards() {
        return hand.getCards();
    }
    
    public boolean isBlackjack() {
    	return hand.isBlackjack();
    }
    
    public int calculateHand() {
    	return hand.calculateHand();
    }
    
    public boolean isSplit() {
    	return hand.isSplit();
    }

}
