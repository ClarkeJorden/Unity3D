package BlackjackExtension;

import java.util.ArrayList;
import java.util.List;

import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.extensions.SFSExtension;

import BlackjackEngine.Action;
import BlackjackEngine.Card;
import BlackjackEngine.Player;
import BlackjackEngine.Result;
import BlackjackEngine.Table;

public class RoomExtension extends SFSExtension {
	
	public int tableSize = 3;
	public int minBuyin, maxBuyin;
	public int minBet, maxBet;
	public boolean isFast = true;
	public int waitTime = 10;
	
	private Table table;

    @Override
    public void init() {
	    addRequestHandler("blackjack_ready", ReadyHandler.class);
	    addRequestHandler("blackjack_join", JoinHandler.class);
	    addRequestHandler("blackjack_action", ActionHandler.class);
    	
    	tableSize = 3;
    	minBuyin = 50;	maxBuyin = 30000;
    	minBet = 1;		maxBet = 300;
    	
    	table = new Table(this);
    }
    
    public void joinPlayer(ISFSObject objIn)
    {
    	String email = objIn.getUtfString("email");
    	String name = objIn.getUtfString("name");
    	int pos = objIn.getInt("pos");
    	int amount = objIn.getInt("amount");
    	table.players.add(new Player(pos, email, name, amount));
    	if(!table.isRunning)
    		table.run();
    }
    
    public void sendBet(Player playerToNotify)
    {
    	int minB = minBet;
    	int maxB = (maxBet < playerToNotify.getCash()) ? maxBet : playerToNotify.getCash();
    	ISFSObject obj = new SFSObject();
    	obj.putInt("min", minB);
    	obj.putInt("max", maxB);
    	if(getParentRoom().getUserByName(playerToNotify.getEmail()) != null)
    		send("blackjack_bet", obj, getParentRoom().getUserByName(playerToNotify.getEmail()));
    }
    
    public void getAction(ISFSObject objIn)
    {
    	int pos = objIn.getInt("pos");
    	int action = objIn.getInt("action");
    	int amount = objIn.getInt("amount");
    	for(Player player : table.players) {
    		if(player.getPos() == pos) {
    			if(action == 0) {			// Bet
    				player.action = Action.BET;
    				player.setBet(amount);
    				player.payCash(amount);
    				doAnimation("Bet", pos, amount);
    			} else if(action == 1) {	// Stand
    				player.action = Action.STAND;
    			} else if(action == 2) {	// Hit
    				player.action = Action.HIT;
    			} else if(action == 3) {	// Double
    				player.action = Action.DOUBLE;
    				doAnimation("Double", pos, player.getBet());
    			} else if(action == 4) {	// Split
    				player.action = Action.SPLIT;
    			}
    		}
    	}
    	
    	boolean isRemain = false;
    	for(Player player : table.players) {
    		if(player.result != Result.NEW && player.result != Result.BUSTED && player.action == Action.NONE) {
    			isRemain = true;
    		}
    	}
    	if(!isRemain) {
			table.delayFlag = false;
			synchronized (table.monitor) {
				table.monitor.notifyAll();
			}
    	}
    }
    
    public void sendAction(Player playerToNotify)
    {
    	List<Integer> actionList = new ArrayList<>();
		actionList.add(1);		// Stand
		actionList.add(2);		// Hit
    	if(playerToNotify.action == Action.BET) {
    		actionList.add(3);		// Double
//    		if(playerToNotify.isSplit())
//    			actionList.add(4);	// Split
    	}
    	ISFSObject obj = new SFSObject();
    	obj.putIntArray("action_list", actionList);
    	if(getParentRoom().getUserByName(playerToNotify.getEmail()) != null)
    		send("blackjack_action", obj, getParentRoom().getUserByName(playerToNotify.getEmail()));
    }
    
    public void updatePlayers()
    {
    	ISFSObject resObj = new SFSObject();
    	ISFSArray array = new SFSArray();
    	for(Player player : table.players) {
    		ISFSObject obj = new SFSObject();
    		obj.putInt("pos", player.getPos());
    		obj.putUtfString("name", player.getName());
    		obj.putInt("cash", player.getCash());
    		obj.putInt("bet", player.getBet());
    		obj.putUtfString("action", player.action.toString());
    		obj.putUtfString("result", player.result.toString());
    		
    		Card[] cards = player.getCards();
    		obj.putInt("card_num", cards.length);
    		for(int i = 0; i < cards.length; i ++)
    			obj.putInt("card" + i, cards[i].hashCode());
    		array.addSFSObject(obj);
    	}
    	
    	Card[] dealCards = table.dealer.getCards();
    	resObj.putBool("dealer_show", table.dealer.isDealerShow);
    	resObj.putInt("dealer_card_num", dealCards.length);
		for(int i = 0; i < dealCards.length; i ++) 
			resObj.putInt("dealer_card" + i, dealCards[i].hashCode());
		
		if(getParentRoom().getUserList().size() > 0)
			send("blackjack_update", resObj, getParentRoom().getUserList());
    }
    
    public void doAnimation(String act, int pos, int value)
    {
    	updatePlayers();
    	ISFSObject obj = new SFSObject();
    	obj.putUtfString("action", act);
    	obj.putInt("pos", pos);
    	obj.putInt("value", value);
		if(getParentRoom().getUserList().size() > 0)
			send("blackjack_animation", obj, getParentRoom().getUserList());
    }

}
