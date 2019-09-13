package BlackjackEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import BlackjackExtension.RoomExtension;

public class Table {
	
	private final RoomExtension gameExt;

	public List<Player> players;
	public Player dealer;
	public Deck deck;
    
    public boolean isRunning = false;
    public State state;

    public Timer timer;
    public boolean delayFlag;
    public final Object monitor = new Object();

    public Table(RoomExtension roomExt) {
		this.gameExt = roomExt;
		players = new ArrayList<>();
	}

    private void delayTimer(float _t) {
    	delayFlag = true;
		// SetTimer
    	timer = new Timer();
		timer.schedule(new TimerTask() {
			  @Override
			  public void run() {
			    // Your database code here
				  if(delayFlag) {
					  delayFlag = false;
					  synchronized (monitor) {
						  monitor.notifyAll();
					  }
				  }
			  }
			}, (int)(_t*1000));
		
		while(delayFlag) {
            // Wait for the user to select an action.
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }
		}
        timer.cancel();
    }
    
    public void run()
    {
    	isRunning = true;
    	while(players.size() > 0) {
    		int activePlayerNum = 0;
    		for(Player player : players) {
    			if(player.getCash() < gameExt.minBet)
    				player.result = Result.BUSTED;
    			else {
    				activePlayerNum ++;
    				player.result = Result.NONE;
    			}
    		}
    		Collections.sort(players);
    		if(activePlayerNum > 0)
    			playHand();
    		else
    			break;
    	}
    	isRunning = false;
    }
    
    public void playHand()
    {
    	resetHand();
    	doBetting();
    	dealCards();
    	doPlayerAct();
    	doDealerAct();
    	doPayout();
    	doClear();
    }
    
    public void resetHand()
    {
    	state = State.BETTING;
    	deck.shuffle();
    	dealer.resetHand();
    	for(Player player : players) {
    		player.resetHand();
    	}
    	gameExt.updatePlayers();
    }
    
    public void doBetting()
    {
    	for(Player player : players) {
    		if(player.result == Result.NEW || player.result == Result.BUSTED)
    			continue;
    		gameExt.sendBet(player);
    	}
    	delayTimer(10f);
    	for(Player player : players) {
    		if(player.result != Result.NEW && player.action == Action.NONE) {
    			player.result = Result.FOLD;
    		}
    	}
    	gameExt.updatePlayers();
    }
    
    public void dealCards()
    {
    	for(int i = 0; i < 2; i ++) {
        	for(Player player : players) {
        		if(player.result == Result.NEW || player.result == Result.BUSTED)
        			continue;
        		player.hand.addCard(deck.deal());
        		gameExt.doAnimation("Deal", player.getPos(), i);
        		delayTimer(1);
        		if(player.isBlackjack())
        			player.result = Result.DEALER_LOSE;
        	}
    		dealer.hand.addCard(deck.deal());
    		gameExt.doAnimation("Deal", 3, i);
    	}
    }
    
    public void doPlayerAct()
    {
    	for(Player player : players) {
    		if(player.result == Result.NEW || player.result == Result.BUSTED)
    			continue;
    		while(player.result == Result.NONE) {
    			if(player.action == Action.BET || player.action == Action.HIT) {
    				doHittingRound(player);
    			}
    			
    			int value = player.calculateHand();
    			if(value > 21) {
    				player.result = Result.PLYAER_LOSE;
	        		gameExt.doAnimation("Fold", player.getPos(), 0);
	        		delayTimer(1);
	        		player.hand.removeAllCards();
    				break;
    			}
    			
    			gameExt.updatePlayers();

    			if(player.action == Action.STAND) {
    				break;
    			} else if(player.action == Action.DOUBLE) {
    				// make double
    				player.action = Action.STAND;
    			} else if(player.action == Action.HIT) {
    				player.hand.addCard(deck.deal());
	        		gameExt.doAnimation("Deal", player.getPos(), player.hand.size() - 1);
    				delayTimer(1);
    			} else if(player.action == Action.SPLIT) {
    				Card[] cards = player.getCards();
    				player.hand.removeAllCards();
    				player.hand.addCard(cards[1]);
    				player.hand1.addCard(cards[0]);
    				delayTimer(2);
    			}
    		}
    	}
    }
    
    public void doHittingRound(Player player)
    {
    	gameExt.sendAction(player);
    	player.action = Action.NONE;
    	delayTimer(10f);
    	if(player.action == Action.NONE)
    		player.action = Action.STAND;
    	gameExt.updatePlayers();
    }
    
    public void doDealerAct()
    {
    	boolean isRemain = false;
    	for(Player player : players) {
    		if(player.result != Result.NEW && player.result != Result.BUSTED && player.result == Result.NONE && player.action == Action.STAND)
    			isRemain = true;
    	}
    	
		gameExt.doAnimation("Show", 3, 1);
    	delayTimer(1);
    	
    	while(isRemain && dealer.calculateHand() < 17) {
    		dealer.hand.addCard(deck.deal());
    		gameExt.doAnimation("Show", 3, dealer.hand.size() - 1);
    		delayTimer(1);
    	}
    	if(dealer.calculateHand() > 21) {
    	}
    }
    
    public void doPayout()
    {
		int dealerVal = dealer.calculateHand();
    	for(Player player : players) {
    		if(player.result == Result.NEW || player.result == Result.BUSTED || player.result == Result.PLYAER_LOSE || player.result == Result.FOLD)
    			continue;
    		int cash = player.getCash();
    		int bet = player.getBet();
    		delayTimer(1);
    		if(player.result == Result.DEALER_LOSE) {
    			if(player.isBlackjack()) {
	        		gameExt.doAnimation("DealerLose", player.getPos(), bet + bet / 2);
	        		delayTimer(1);
    				player.setCash(cash + bet + bet / 2);
    				player.setBet(0);
    			} else {
    			}
    		} else if(player.result == Result.NONE && player.action == Action.STAND) {
    			int playerVal = player.calculateHand();
    			if(dealerVal > 21 || dealerVal < playerVal) {
	        		gameExt.doAnimation("PlayerLose", player.getPos(), bet);
	        		delayTimer(1);
    				player.setBet(0);
    			} else {
	        		gameExt.doAnimation("DealerLose", player.getPos(), bet);
	        		delayTimer(1);
    				player.setCash(cash + bet);
    				player.setBet(0);
    			}
    		}
    		gameExt.updatePlayers();
    	}
    }
    
    public void doClear()
    {
    	resetHand();
    	for(int i = players.size() - 1; i >= 0; i --)
    	{
    		Player player = players.get(i);
    		if(player.hand.size() > 0) {
        		gameExt.doAnimation("Fold", player.getPos(), 0);
        		delayTimer(1);
        		player.hand.removeAllCards();
    		}
    	}
		gameExt.doAnimation("Fold", 3, 0);
		delayTimer(1);
		dealer.hand.removeAllCards();
    }
    
}
