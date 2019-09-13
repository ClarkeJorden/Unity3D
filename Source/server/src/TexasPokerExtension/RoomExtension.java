package TexasPokerExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import TexasPokerEngine.Card;
import TexasPokerEngine.Client;
import TexasPokerEngine.Hand;
import TexasPokerEngine.HandValue;
import TexasPokerEngine.PlayMode;
import TexasPokerEngine.Player;
import TexasPokerEngine.PlayerStatus;
import TexasPokerEngine.Table;
import TexasPokerEngine.TableType;
import TexasPokerAction.Action;
import TexasPokerAction.BetAction;
import TexasPokerAction.RaiseAction;
import TexasPokerBot.BasicBot;
import ZoneExtension.ZoneExtension;
import ZoneExtension.ZoneExtension.DynamicRoomType;
import TexasPokerExtension.LogOutput;

import com.smartfoxserver.v2.api.CreateRoomSettings;
import com.smartfoxserver.v2.api.CreateRoomSettings.RoomExtensionSettings;
import com.smartfoxserver.v2.core.SFSEventType;
import com.smartfoxserver.v2.db.IDBManager;
import com.smartfoxserver.v2.entities.Room;
import com.smartfoxserver.v2.entities.SFSRoomRemoveMode;
import com.smartfoxserver.v2.entities.User;
import com.smartfoxserver.v2.entities.Zone;
import com.smartfoxserver.v2.entities.data.ISFSArray;
import com.smartfoxserver.v2.entities.data.ISFSObject;
import com.smartfoxserver.v2.entities.data.SFSArray;
import com.smartfoxserver.v2.entities.data.SFSObject;
import com.smartfoxserver.v2.entities.variables.RoomVariable;
import com.smartfoxserver.v2.entities.variables.SFSRoomVariable;
import com.smartfoxserver.v2.exceptions.SFSCreateRoomException;
import com.smartfoxserver.v2.extensions.BaseSFSExtension;
import com.smartfoxserver.v2.extensions.ExtensionLogLevel;
import com.smartfoxserver.v2.extensions.ISFSExtension;
import com.smartfoxserver.v2.extensions.SFSExtension;

public class RoomExtension extends SFSExtension implements Client {

	public Table table;

	private static final TableType TABLE_TYPE = TableType.NO_LIMIT;

	private int blindType = 0;
	private int tableSize = 9;
	private boolean isEmpty = false;
	private boolean isFast = true;
	public BigDecimal minBuyin = BigDecimal.valueOf(500);
	public BigDecimal maxBuyin = BigDecimal.valueOf(10000);
	public BigDecimal bigBlind = BigDecimal.valueOf(10);

	private int BOT_WAITING_TIME = 1;
	private int USER_WAITING_TIME = 20;
	private int CARD_BACK_NUM = 52;
	private int CARD_PLACEHOLDER_NUM = 53;

	private Player humanPlayer;
	private String dealerName;
	private int dealerPos;
	private String actorName;
	private int actorPos;

	private Timer timer;
	private TimerTask task;
	private Action selectedAction;
	private final Object monitor = new Object();
	private boolean prevShow = false;
	int nActNum;
	public String roomName;

	private String dailyHandError;
	
	private DynamicRoomType dynamicRoomType = DynamicRoomType.RT_DEFAULT;
	// for debugging
	public String whereis() {
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		String where = this.getGameRoom().getName() + ": " + ste.getClassName() + " " + ste.getMethodName() + " " + ste.getLineNumber() + " ";
//		System.out.println(where);
		return where;
	}

	public static class TableInfo {
		public long blind;
		public long minBuyin, maxBuyin;

		public TableInfo(long b, long minb, long maxb) {
			blind = b;
			minBuyin = minb;
			maxBuyin = maxb;
		}
	}

	private static TableInfo[] TEXAS_INFO = { new TableInfo(2, 20, 40), new TableInfo(10, 100, 1000),
			new TableInfo(50, 500, 20000), new TableInfo(200, 2000, 50000), new TableInfo(1000, 10000, 200000),
			new TableInfo(4000, 40000, 1000000), new TableInfo(20000, 200000, 4000000),
			new TableInfo(100000, 1000000, 20000000), new TableInfo(500000, 5000000, 100000000),
			new TableInfo(1000000, 10000000, 400000000), new TableInfo(2000000, 100000000, 1000000000),
			new TableInfo(10000000, 200000000, 2000000000), new TableInfo(20000000, 400000000, 4000000000l),
			new TableInfo(50000000, 1000000000, 10000000000l), new TableInfo(100000000, 2000000000, 20000000000l)};
	
	@Override
	public void init() {
		addRequestHandler("texas_ready", ReadyHandler.class);
		addRequestHandler("texas_join", JoinHandler.class);
		addRequestHandler("texas_action", ActionHandler.class);
		addRequestHandler("texas_leave", TexasLeaveHandler.class);
		addRequestHandler("texas_sitout", SitOutHandler.class);
		addRequestHandler("texas_setrebuy", SetRebuyHandler.class);
		addRequestHandler("texas_chat", ChatHandler.class);
		addRequestHandler("texas_addchip", AddChipHandler.class);
		addRequestHandler("texas_gift", GiftHandler.class);
		addRequestHandler("texas_anim_emoji", AnimEmojiHandler.class);

		addEventHandler(SFSEventType.USER_LEAVE_ROOM, LeaveHandler.class);
		addEventHandler(SFSEventType.USER_DISCONNECT, LeaveHandler.class);

		isFast = true;
		USER_WAITING_TIME = isFast ? 20 : 20;

		tableSize = this.getParentRoom().getVariable("table_size").getIntValue();
		blindType = this.getParentRoom().getVariable("blind_type").getIntValue();
		RoomVariable emptyVal = this.getParentRoom().getVariable("empty");
		if (emptyVal != null)
			isEmpty = emptyVal.getBoolValue();
		
		RoomVariable tableType = this.getParentRoom().getVariable("dynamic_table_type");
		
		if(tableType != null)
		{
			String strTableType = tableType.getStringValue();
			if(strTableType == DynamicRoomType.RT_PRIVATE.toString())
				dynamicRoomType = DynamicRoomType.RT_PRIVATE;
			else if(strTableType == DynamicRoomType.RT_AUTO_CREATE.toString())
				dynamicRoomType = DynamicRoomType.RT_AUTO_CREATE;
		}
		
			
		roomName = this.getGameRoom().getName();

		bigBlind = BigDecimal.valueOf(TEXAS_INFO[blindType].blind);
		minBuyin = BigDecimal.valueOf(TEXAS_INFO[blindType].minBuyin);
		maxBuyin = BigDecimal.valueOf(TEXAS_INFO[blindType].maxBuyin);

		table = new Table(PlayMode.NORMAL_MODE, TABLE_TYPE, tableSize, bigBlind, this);

		if (!isEmpty) {
			Random rd = new Random();
			BigDecimal botBuyin = maxBuyin.multiply(BigDecimal.valueOf(3));
			if(tableSize == 5) {
				int pos1 = rd.nextInt(2), pos2 = rd.nextInt(2) + 2;
				table.players[pos1].join(true, true, new BasicBot(0, 25), pos1, "Bot1", "Guest", 9, botBuyin, -1, 0);
				table.players[pos2].join(true, true,  new BasicBot(25, 60), pos2, "Bot2", "Guest", 10, botBuyin, -1, 0);
				table.players[4].join(true, true, new BasicBot(25, 90), 4, "Bot3", "Guest", 11, botBuyin, -1, 0);
			}
			else if(tableSize == 9) {
				int pos1 = rd.nextInt(5), pos2 = rd.nextInt(5) + 4;
				int botNum = 0;
				for(int i=0; i<9; i++) {
					if(i != pos1 && i != pos2) {
						botNum++;
						String botName = "Bot" + botNum;
						int tight = rd.nextInt(40);
						int agree = tight + rd.nextInt(60);
						table.players[i].join(true, true, new BasicBot(tight, agree), i, botName, "Guest", 8 + botNum, botBuyin, -1, 0);
					}
				}
			}
		}
	}
	
	private static long getBlind(int blindType)
	{
		if(blindType < TEXAS_INFO.length)
			return TEXAS_INFO[blindType].blind;
		else 
			return TEXAS_INFO[0].blind;
	}

	@Override
	public Object handleInternalMessage(String cmdName, Object params) {
		if (cmdName.equals("get_room_info")) {
			if(isPrivate())
				return null;
			ISFSObject obj = new SFSObject();
			obj.putUtfString("name", getParentRoom().getName());
			obj.putInt("type", 0); // TexasPoker
			obj.putLong("blind", bigBlind.longValue());
			obj.putLong("min_buyin", minBuyin.longValue());
			obj.putLong("max_buyin", maxBuyin.longValue());
			obj.putLong("pot", table.getTotalChip().longValue());
			obj.putInt("size", tableSize);
			obj.putInt("player_num", table.playerSize());
			obj.putInt("bot_player_num", table.botPlayerSize());
			obj.putBool("speed", isFast);
			boolean isEmpty = true;
			ISFSArray playerList = new SFSArray();
			for (Player player : table.players) {
				if (player.playerStatus != PlayerStatus.NONE) {
					ISFSObject obj1 = new SFSObject();
					obj1.putInt("pos", player.getPos());
					obj1.putUtfString("email", player.getEmail());
					obj1.putUtfString("name", player.getName());
					obj1.putLong("chip", player.getCash().longValue());
					playerList.addSFSObject(obj1);
					if(!player.isBot())
						isEmpty = false;
				}
			}
			obj.putBool("is_empty", isEmpty);
			// for(Player player : table.newPlayers) {
			// ISFSObject obj1 = new SFSObject();
			// obj1.putInt("pos", player.getPos());
			// obj1.putUtfString("name", player.getName());
			// playerList.addSFSObject(obj1);
			// }
			obj.putSFSArray("player_list", playerList);

			return obj;
		} else if (cmdName.equals("get_user_balance")) {
			for (Player player : table.players) {
				if (player.playerStatus != PlayerStatus.NONE) {
					if (player.getEmail().compareTo((String) params) == 0) {
						ISFSObject obj = new SFSObject();
						obj.putLong("user_balance", player.getCash().longValue());
						return obj;
					}
				}
			}
			return null;
		}
		else if(cmdName.equals("get_user_room_info")) {
			for (Player player : table.players) {
				if (player.playerStatus != PlayerStatus.NONE) {
					if (player.getEmail().compareTo((String) params) == 0) {
						ISFSObject obj = new SFSObject();
						obj.putUtfString("name", getParentRoom().getName());
						obj.putInt("type", 0); // TexasPoker
						obj.putLong("blind", bigBlind.longValue());
						obj.putLong("min_buyin", minBuyin.longValue());
						obj.putLong("max_buyin", maxBuyin.longValue());
						obj.putInt("size", tableSize);
						
						return obj;
					}
				}
			}			
			return null;
		}
		else if(cmdName.equals("set_user_can_leave")) {
			ISFSObject obj = (ISFSObject) params;
			String email = obj.getUtfString("email");
			boolean canLeave = obj.getBool("can_leave");
			
			for (Player player : table.players) {
				if (player.playerStatus != PlayerStatus.NONE) {
					if (player.getEmail().compareTo(email) == 0) {
						player.canLeave = canLeave;
						return true;
					}
				}
			}			
			return false;
		}

		return null;
	}

	public void sendPlayerInputRequest(Player player){
		Set<Action> allowedActions = table.getAllowedActions(player);
		BigDecimal callAmount = table.minBet.subtract(player.getBet());
		BigDecimal minBetAmount = callAmount.add(table.minBetDiff);
		BigDecimal maxBetAmount = table.getMaxBetAmount(player);

		ISFSObject resObj = new SFSObject();
		resObj.putLong("pot", player.getBet().longValue());
		resObj.putLong("call_amount", callAmount.longValue());
		resObj.putLong("min_bet", minBetAmount.longValue());
		resObj.putLong("max_bet", maxBetAmount.longValue());
		resObj.putInt("action_size", allowedActions.size());
		resObj.putLong("cash", player.getCash().longValue());
		int i = 0;
		for (Action action : allowedActions) {
			resObj.putUtfString("action" + i, action.getName());
			i++;
		}
		if(!player.isAllIn())
			send("texas_userinput", resObj, getParentRoom().getUserByName(player.getEmail()));
	}
	
	public void readyPlayer(String email, boolean isAutomatic, int sitPos) {
		for (Player player : table.players) {
			if (player.playerStatus != PlayerStatus.NONE) {
				if (email.compareTo(player.getEmail()) == 0) {
					player.canLeave = true;
					ISFSObject obj = new SFSObject();
					obj.putInt("pos", player.getPos());
					if (getParentRoom().getUserByName(email) != null) {
						send("texas_ready", obj, getParentRoom().getUserByName(email));
						
						if(player == table.actor) {
							sendPlayerInputRequest(player);
						}
					}
				}
			}
		}
		// for(Player player : table.newPlayers) {
		// if(email.compareTo(player.getEmail()) == 0) {
		// ISFSObject obj = new SFSObject();
		// obj.putInt("pos", player.getPos());
		// if(getParentRoom().getUserByName(email) != null)
		// send("texas_ready", obj, getParentRoom().getUserByName(email));
		// }
		// }
		if (isAutomatic) {
			if(sitPos == -1) {
				for (int i = 0; i < tableSize; i++) {
					boolean flag = true;
					for (Player player : table.players) {
						if (player.playerStatus != PlayerStatus.NONE) {
							if (player.getPos() == i)
								flag = false;
						}
					}
					// for(Player player : table.newPlayers) {
					// if(player.getPos() == i)
					// flag = false;
					// }
					if (flag) {
						ISFSObject obj = new SFSObject();
						obj.putInt("pos", i);
						if (getParentRoom().getUserByName(email) != null)
							send("texas_autojoin", obj, getParentRoom().getUserByName(email));
						break;
					}
				}
			}
			else if(sitPos < tableSize){
				boolean flag = true;
				for (Player player : table.players) {
					if (player.playerStatus != PlayerStatus.NONE) {
						if (player.getPos() == sitPos)
							flag = false;
					}
				}
				if (flag) {
					ISFSObject obj = new SFSObject();
					obj.putInt("pos", sitPos);
					if (getParentRoom().getUserByName(email) != null)
						send("texas_autojoin", obj, getParentRoom().getUserByName(email));
				}
			}
		}

		// for debug by jbj 20180904
		whereis();
		////////////////////////////

		updateAll();
		if (!table.isRunning()) {
			if (table.playerSize() >= 2)
			{
				new Thread(new Runnable() {
				     public void run() {
						table.run();
				     }
				}).start();
			}
		}
	}

	public void addChip(ISFSObject params) {
		String email = params.getUtfString("email");
		long chip = params.getLong("chip");
		for (Player player : table.players) {
			if (player.playerStatus != PlayerStatus.NONE) {
				if (player.getEmail().compareTo(email) == 0) {
					if (player.playerStatus == PlayerStatus.ACTIVE && player.isActive) {
						player.isAddChip = true;
						player.newChip = chip;
					} else {
						player.newChip = chip;
						// System.out.println("chip: " + player.getCash() + ", newChip: " + chip);
						sendNewChip(player);
						if (player.playerStatus == PlayerStatus.SITOUT)
							sitoutPlayer(player.getEmail(), false);
						else
							updatePlayer(player, false, false);
					}
					return;
				}
			}
		}
	}

	public void sendNewChip(Player player) {
	 	//for log trace
	 	LogOutput.traceLog("[sendNewChip] begins");
	 	//trace("[sendNewChip] begins");	 	
	 	
		long diff = player.newChip - player.getCash().longValue();
		if (!player.isBot())
			payPlayerChipcount(player.getEmail(), diff);
		player.isAddChip = false;
		player.setCash(BigDecimal.valueOf(player.newChip));
		player.setAction(Action.ANTE);

		ISFSObject obj = new SFSObject();
		obj.putInt("pos", player.getPos());
		obj.putLong("diff", diff);
		if (getParentRoom().getUserList().size() > 0)
			send("texas_addchip", obj, getParentRoom().getUserList());
		
		//for log trace
	 	LogOutput.traceLog("[sendNewChip] ends");
	 	//trace("[sendNewChip] ends");
	}

	public void sendRebuy(Player player) {
		//for log trace
	 	LogOutput.traceLog("[sendRebuy] begins");
	 	//trace("[sendRebuy] begins");
		if (!player.isBot() && getParentRoom().getUserByName(player.getEmail()) != null)
		{
			long amount = getPlayerChipcount(player.getEmail());
			int blindType = this.getParentRoom().getVariable("blind_type").getIntValue();
			long minBuyin = TEXAS_INFO[blindType].minBuyin;
			ISFSObject obj = new SFSObject();
			obj.putLong("Chip", amount);
			obj.putLong("minBuyin",minBuyin);
			obj.putInt("blindtype", blindType);
			send("texas_rebuy",obj,getParentRoom().getUserByName(player.getEmail()));
		}
		//for log trace
	 	LogOutput.traceLog("[sendRebuy] ends");
	 	//trace("[sendRebuy] ends");
	}

	public void setRebuy(String email, boolean isRebuy) {
		for (Player player : table.players) {
			if (player.playerStatus == PlayerStatus.NONE)
				continue;
			if (email.compareTo(player.getEmail()) == 0) {
				player.isAutoRebuy = isRebuy;
			}
		}
	}

	public boolean joinNewPlayer(ISFSObject params) {
		int pos = params.getInt("pos");
		String email = params.getUtfString("email");
		String name = params.getUtfString("name");
		long buyin = params.getLong("buyin");
		boolean isAutoRebuy = params.getBool("rebuy");

		ISFSObject info = getUserInfo(email);

		int giftCategory = info.getInt("gift_category");
		int giftValue = info.getInt("gift_value");

		int i = 0;

		if (table.playerSize() == tableSize)
			return false;
		addPlayerChipcount(email, -buyin);

		table.players[pos].join(false, isAutoRebuy, this, pos, email, name, 0, BigDecimal.valueOf(buyin), giftCategory,
				giftValue);

		updateAll();

		if (getParentRoom().getUserList().size() > 0)
			send("texas_join", params, getParentRoom().getUserList());
		sendMessage("", "Dealer", name + " has joined the game.", true);

		if (!table.isRunning()) {
			if (table.playerSize() >= 2)
			{
				new Thread(new Runnable() {
				     public void run() {
						table.run();
				     }
				}).start();
			}
		}
		return true;
	}
	
	public void autoCreateTexasRooms()
	{
		if(!isPrivate())
		{
			autoCreateTexasRooms(getParentZone(), blindType, tableSize, isFast, isEmpty);
		}
	}
	public void autoDeleteEmptyRoom()
	{
		autoDeleteTexasRooms(getParentZone(), blindType, tableSize, isFast, isEmpty);
	}
	
	public boolean leavePlayer(String email, int status) {
		boolean ret = true;
		for (Player player : table.players) {
			if (player.playerStatus == PlayerStatus.NONE)
				continue;
			if (email.compareTo(player.getEmail()) == 0) {
				if(player.canLeave) {
					if (player.isActive) {
						if (table.actor == player) {
							setSelectedAction(1, 0);
						} else {
							player.isActive = false;
							// table.activePlayers.remove(player);
							table.playersToAct--;
						}
					}
	
					// player.playerStatus = PlayerStatus.NONE;
					
					// for debug by jbj 20180904
					whereis();
					////////////////////////////
					player.leave();
					removePlayer(player, status);
					// for debug by jbj 20180904
					whereis();
					////////////////////////////
				}
				else {
					ret = false;
				}
			}
		}
		
		return ret;
	}

	public void removePlayer(Player player, int status) {
		if (!player.isBot())
			addPlayerChipcount(player.getEmail(), player.getCash().longValue());
		if (getParentRoom().getUserList().size() > 0) {
			ISFSObject obj = new SFSObject();
			obj.putInt("pos", player.getPos());
			obj.putUtfString("email", player.getEmail());
			obj.putUtfString("name", player.getName());
			obj.putInt("status", status);
			send("texas_leave", obj, getParentRoom().getUserList());
		}
		sendMessage("", "Dealer", player.getName() + " has left the game.", true);
		showBestCards();
	}

	public void sitoutPlayer(String email, boolean isSitOut) {
		for (Player player : table.players) {
			if (player.playerStatus == PlayerStatus.NONE)
				continue;
			if (email.compareTo(player.getEmail()) == 0) {
				if (player.isActive) {
					if (table.actor == player) {
						setSelectedAction(1, 0);
					} else {
						player.isActive = false;
						// table.activePlayers.remove(player);
						table.playersToAct--;
					}
				}

				if (isSitOut) {
					// System.out.println("sitout start");
					player.playerStatus = PlayerStatus.SITOUT;
					player.resetHand();
					player.setAction(Action.SIT_OUT);
					// System.out.println(player.playerStatus.toString());
				} else {
					player.playerStatus = PlayerStatus.ACTIVE;
					player.resetHand();
					player.setAction(null);
				}
			}
		}
		// for debug by jbj 20180904
		whereis();
		////////////////////////////
		updateAll();
	}

	public void updateAll() {
		updateBoard(0, false);
		updatePlayers(prevShow, false);
//		setDealer(table.dealer.getPos());
		showBestCards();
		if (table.isRunning()) {
			setBlind();
			selectActor(table.actor, table.isSelect, true);
		}
	}

	public void sendChipStatus(String _email) {
		if (getParentRoom().getUserByName(_email) == null)
			return;
		long amount = getPlayerChipcount(_email);
		ISFSObject obj = new SFSObject();
		obj.putLong("chip", amount);
		send("texas_chipstatus", obj, getParentRoom().getUserByName(_email));
	}

	/**
	 * The application's entry point.
	 * 
	 * @param args
	 *            The command line arguments.
	 */

	@Override
	public void joinedTable(Player playerToNotify, TableType type, BigDecimal bigBlind, List<Player> players) {
		for (Player player : players) {
			playerUpdated(playerToNotify, player);
		}
	}

	@Override
	public void messageReceived(Player playerToNotify, String message) {
		// sendMessage(playerToNotify.getEmail(), message);
	}

	public void sendMessage(String message) {
		ISFSObject resObj = new SFSObject();
		resObj.putUtfString("text", message);
		if (getParentRoom().getUserList().size() > 0)
			send("texas_settext", resObj, getParentRoom().getUserList());
	}

	public void sendMessage(String email, String name, String message, boolean isDealer) {
		ISFSObject resObj = new SFSObject();
		resObj.putUtfString("email", email);
		resObj.putUtfString("name", name);
		resObj.putUtfString("message", message);
		resObj.putBool("dealer", isDealer);

		int pos = -1;
		for (Player player : table.players) {
			if (player.playerStatus == PlayerStatus.NONE)
				continue;
			if (player.getEmail().compareTo(email) == 0)
				pos = player.getPos();
		}
		resObj.putInt("pos", pos);

		if (getParentRoom().getUserList().size() > 0)
			send("texas_chat", resObj, getParentRoom().getUserList());
	}

	@Override
	public void handStarted(Player playerToNotify, Player dealer) {
		setDealer(playerToNotify, dealerPos, false);
		dealerPos = dealer.getPos();
		dealerName = dealer.getName();
		setDealer(playerToNotify, dealer.getPos(), true);
	}

	public void dealCards() {
		//for log trace
	 	LogOutput.traceLog("[dealCards] begins");
	 	//trace("[dealCards] begins");
		if (getParentRoom().getUserList().size() > 0)
			send("texas_deal_cards", new SFSObject(), getParentRoom().getUserList());
		//for log trace
	 	LogOutput.traceLog("[dealCards] ends");
	 	//trace("[dealCards] ends");
	}

	public void setDealer(int pos) {
		ISFSObject resObj = new SFSObject();
		resObj.putInt("pos", pos);
		if (getParentRoom().getUserList().size() > 0)
			send("texas_setdealer", resObj, getParentRoom().getUserList());
	}

	public void setBlind(int _pos, int type) {
		ISFSObject resObj = new SFSObject();
		resObj.putInt("pos", _pos);
		resObj.putInt("type", type);
		// resObj.putUtfString("blind_text", blindText);
		if (getParentRoom().getUserList().size() > 0)
			send("texas_setblind", resObj, getParentRoom().getUserList());
	}

	public void setBlind(Player blind, int type) {
		ISFSObject resObj = new SFSObject();
		resObj.putInt("pos", blind.getPos());
		resObj.putInt("type", type);
		// resObj.putUtfString("blind_text", blindText);
		if (getParentRoom().getUserList().size() > 0)
			send("texas_setblind", resObj, getParentRoom().getUserList());
	}

	@Override
	public void setBlind(Player playerToNotify, Player blind, String blindText) {
		if (getParentRoom().getUserByName(playerToNotify.getEmail()) == null)
			return;
		ISFSObject resObj = new SFSObject();
		resObj.putInt("pos", blind.getPos());
		resObj.putUtfString("blind_text", blindText);
		send("texas_setblind", resObj, getParentRoom().getUserByName(playerToNotify.getEmail()));
	}

	public void setBlind() {
		//for log trace
	 	LogOutput.traceLog("[setBlind] begins");
	 	//trace("[setBlind] begins");
		ISFSObject resObj = new SFSObject();
		resObj.putInt("pos0", table.dealerPos);
		resObj.putInt("pos1", table.smallBlindPos);
		resObj.putInt("pos2", table.bigBlindPos);
		// System.out.println("Blind:" + table.dealerPos + "," + table.smallBlindPos +
		// "," + table.bigBlindPos);
		if (getParentRoom().getUserList().size() > 0)
			send("texas_setblind", resObj, getParentRoom().getUserList());	
		//for log trace
	 	LogOutput.traceLog("[setBlind] ends");
	 	//trace("[setBlind] ends");
	}

	@Override
	public void actorRotated(Player playerToNotify, Player actor) {
		setActorInTurn(playerToNotify, actorPos, false);
		actorPos = actor.getPos();
		actorName = actor.getName();
		setActorInTurn(playerToNotify, actorPos, true);
	}

	public void selectActor(Player actor, boolean show, boolean isJoinPlayer) {
		if(actor == null)
			return;
		actorPos = actor.getPos();
		ISFSObject resObj = new SFSObject();
		resObj.putInt("pos", actorPos);
		if(!actor.isAllIn())
			resObj.putBool("show", show);
		else
			resObj.putBool("show", false);
		resObj.putInt("time", actor.isBot() ? BOT_WAITING_TIME : ((isFast) ? 10 : 20));
		resObj.putBool("isJoinPlayer", isJoinPlayer);
		if (getParentRoom().getUserList().size() > 0)
			send("texas_setactor", resObj, getParentRoom().getUserList());
	}

	@Override
	public void selectActor(Player playerToNotify, Player actor, boolean show) {
		if (getParentRoom().getUserByName(playerToNotify.getEmail()) == null)
			return;
		actorPos = actor.getPos();
		ISFSObject resObj = new SFSObject();
		resObj.putInt("pos", actorPos);
		resObj.putBool("show", show);
		resObj.putInt("time", actor.isBot() ? BOT_WAITING_TIME : ((isFast) ? 10 : 20));
		send("texas_setactor", resObj, getParentRoom().getUserByName(playerToNotify.getEmail()));
	}

	public void updateBoard(int dealRound, boolean isLast) {
		//for log trace
	 	LogOutput.traceLog("[updateBoard] begins");
	 	//trace("[updateBoard] begins");
		ISFSObject resObj = new SFSObject();
		List<Integer> cardArray = new ArrayList<Integer>();
		for (Card card : table.board) {
			cardArray.add(card.hashCode());
		}

		long pot = table.getTotalPot().longValue();
		for (Player player : table.players) {
			pot -= player.getBet().longValue();
		}

		resObj.putIntArray("cards", cardArray);
		resObj.putLong("pot", pot);
		resObj.putInt("dealround", dealRound);
		resObj.putBool("isLast", isLast);

		if (getParentRoom().getUserList().size() > 0)
			send("texas_updateboard", resObj, getParentRoom().getUserList());
		//for log trace
	 	LogOutput.traceLog("[updateBoard] ends");
	 	//trace("[updateBoard] ends");
	}

	public void showDealerPot(long dealerShare) {
		//for log trace
	 	LogOutput.traceLog("[showDealerPot] begins");
	 	//trace("[showDealerPort] begins");
		ISFSObject resObj = new SFSObject();
		if (getParentRoom().getUserList().size() > 0)
			send("texas_showdealerpot", resObj, getParentRoom().getUserList());
		//for log trace
	 	LogOutput.traceLog("[showDealerPot] ends");
	 	//trace("[showDealerPort] ends");
	}

	@Override
	public void boardUpdated(Player playerToNotify, List<Card> cards, BigDecimal bet, BigDecimal pot) {
		if (getParentRoom().getUserByName(playerToNotify.getEmail()) == null)
			return;
		playerUpdated(playerToNotify, playerToNotify);

		ISFSObject resObj = new SFSObject();
		List<Integer> cardArray = new ArrayList<Integer>();
		for (Card card : cards) {
			cardArray.add(card.hashCode());
		}
		resObj.putIntArray("cards", cardArray);
		resObj.putLong("pot", pot.longValue());

		send("texas_updateboard", resObj, getParentRoom().getUserByName(playerToNotify.getEmail()));
	}

	public void resetButtons() {
		if (getParentRoom().getUserList().size() > 0)
			send("texas_reset", new SFSObject(), getParentRoom().getUserList());
	}

	@Override
	public void playerUpdated(Player playerToNotify, Player player) {
		updatePlayer(playerToNotify, player, false, false);
	}

	public void updatePlayers(boolean isShow, boolean isDeal) {
		//for log trace
	 	LogOutput.traceLog("[updatePlayers] begins");
	 	//trace("[updatePlayers] begins");
		prevShow = isShow;
		for (Player player : table.players) {
			if (player.playerStatus == PlayerStatus.ACTIVE) {
				// if (!isShow)
				// updatePlayer(player.publicClone(), false, isDeal);
				// else if (player.isShow)
				// updatePlayer(player, false, isDeal);
				// else
				// updatePlayer(player.publicClone(), false, isDeal);
				//
				// if (!player.isBot())
				// updatePlayer(player, player, false, isDeal);				
				updatePlayer(player, false, isDeal);
			} else if (player.playerStatus == PlayerStatus.NEW)
			{
				updatePlayer(player, true, isDeal);
				//trace("case : player.playerStatus == PlayerStatus.NEW");
			}
			else if (player.playerStatus == PlayerStatus.SITOUT)
			{
				updatePlayer(player, false, isDeal);
				//trace("case : player.playerStatus == PlayerStatus.SITOUT");
			}
		}
		showBestCards();
		//for log trace
	 	LogOutput.traceLog("[updatePlayers] ends");
	 	//trace("[updatePlayers] ends");
	}

	void updatePlayer(Player player, boolean isNew, boolean isDeal) {
		//for log trace
	 	LogOutput.traceLog("[updatePlayer] begins");
	 	//trace("[updatePlayer] begins");
		ISFSObject resObj = new SFSObject();
		resObj.putInt("pos", player.getPos());
		resObj.putUtfString("email", player.getEmail());
		resObj.putUtfString("name", player.getName());
		resObj.putInt("avatar", player.getAvatar());
		resObj.putLong("chip", player.getCash().longValue());
		resObj.putLong("balance", player.getCash().longValue() + getPlayerChipcount(player.getEmail()));
		resObj.putInt("raise", player.raise);
		// System.out.println("UPDATE1:" + player.getName() + ":" + player.raise);
		resObj.putLong("pot", player.getBet().longValue());
		resObj.putBool("new", isNew);
		resObj.putBool("sitout", player.playerStatus == PlayerStatus.SITOUT);
		resObj.putBool("active", player.isActive);
		resObj.putBool("deal", isDeal);
		// resObj.putBool("winner", player.isWinner);
		resObj.putInt("gift_category", player.giftCategory);
		resObj.putInt("gift_detail", player.giftDetail);
		resObj.putBool("show", player.isShow);
		resObj.putUtfString("hand_value", player.handValue);
		// System.out.println(player.getEmail() + "," + player.giftCategory + "," +
		// player.giftDetail);
		if(player.isAllIn())
			player.setAction(Action.ALL_IN);
		Action action = player.getAction();
		if (action != null) {
			resObj.putUtfString("action", action.getName());
		} else {
			resObj.putUtfString("action", "");
		}
		if (player.hasCards()) {
			Card[] cards = player.getCards();
			if (cards.length == 2) {
				// Visible cards.
				resObj.putInt("card1", cards[0].hashCode());
				resObj.putInt("card2", cards[1].hashCode());
			} else {
				// Hidden cards (face-down).
				resObj.putInt("card1", CARD_BACK_NUM);
				resObj.putInt("card2", CARD_BACK_NUM);
			}
		} else {
			// No cards.
			resObj.putInt("card1", CARD_PLACEHOLDER_NUM);
			resObj.putInt("card2", CARD_PLACEHOLDER_NUM);
		}

		if (getParentRoom().getUserList().size() > 0)
			send("texas_updateplayer", resObj, getParentRoom().getUserList());
		//for log trace
	 	LogOutput.traceLog("[updatePlayer] ends");
	 	//trace("[updatePlayer] ends");
	}

	void updatePlayer(Player playerToNotify, Player player, boolean isNew, boolean isDeal) {
		ISFSObject resObj = new SFSObject();
		resObj.putInt("pos", player.getPos());
		resObj.putUtfString("email", player.getEmail());
		resObj.putUtfString("name", player.getName());
		 resObj.putInt("avatar", player.getAvatar());
		resObj.putInt("raise", player.raise);
		// System.out.println("UPDATE2:" + player.getName() + ":" + player.raise);
		resObj.putLong("chip", player.getCash().longValue());
		resObj.putLong("balance", player.getCash().longValue() + getPlayerChipcount(player.getEmail()));
		resObj.putLong("pot", player.getBet().longValue());
		resObj.putBool("new", isNew);
		resObj.putBool("sitout", player.playerStatus == PlayerStatus.SITOUT);
		resObj.putBool("active", player.isActive);
		resObj.putBool("deal", isDeal);
		// resObj.putBool("winner", player.isWinner);
		resObj.putInt("gift_category", player.giftCategory);
		resObj.putInt("gift_detail", player.giftDetail);
		resObj.putBool("show", player.isShow);
		resObj.putUtfString("hand_value", player.handValue);
		// System.out.println(player.getEmail() + "," + player.giftCategory + "," +
		// player.giftDetail);
		if(player.isAllIn())
			player.setAction(Action.ALL_IN);
		Action action = player.getAction();
		if (action != null && !player.isAllIn()) {
			resObj.putUtfString("action", action.getName());
		} else {
			resObj.putUtfString("action", "");
		}
		if (player.hasCards()) {
			Card[] cards = player.getCards();
			if (cards.length == 2) {
				// Visible cards.
				resObj.putInt("card1", cards[0].hashCode());
				resObj.putInt("card2", cards[1].hashCode());
			} else {
				// Hidden cards (face-down).
				resObj.putInt("card1", CARD_BACK_NUM);
				resObj.putInt("card2", CARD_BACK_NUM);
			}
		} else {
			// No cards.
			resObj.putInt("card1", CARD_PLACEHOLDER_NUM);
			resObj.putInt("card2", CARD_PLACEHOLDER_NUM);
		}

		if (getParentRoom().getUserByName(playerToNotify.getEmail()) != null)
			send("texas_updateplayer", resObj, getParentRoom().getUserByName(playerToNotify.getEmail()));
	}

	public void showBestCards() {
		//for log trace
	 	LogOutput.traceLog("[showBestCards] begins");
	 	//trace("[showBestCards] begins");
		if (table.showPlayer != null) {
			Hand hand = new Hand(table.board);
			hand.addCards(table.showPlayer.getCards());
			HandValue handValue = new HandValue(hand);
			List<Card> cards = handValue.getBestCards();

			ISFSObject resObj = new SFSObject();
			resObj.putInt("pos", table.showPlayer.getPos());
			resObj.putBool("winner", true);
			resObj.putInt("value", handValue.getType().getValue());
			resObj.putUtfString("description", handValue.getDescription());
			List<Integer> cardArray = new ArrayList<Integer>();
			List<Integer> wholeArray = new ArrayList<Integer>();
			if (table.showPlayer.isShow) {
				for (Card card : cards)
					cardArray.add(card.hashCode());
				for (Card card : handValue.wholeCards)
					wholeArray.add(card.hashCode());
			}
			if(!table.actor.isActive)
				nActNum--;
			resObj.putIntArray("cards", cardArray);
			resObj.putIntArray("whole", wholeArray);
			resObj.putInt("ActNum", nActNum);
			if (getParentRoom().getUserList().size() > 0)
				send("texas_showbestcards", resObj, getParentRoom().getUserList());
			return;
		}
		nActNum = 0;
		for (Player player : table.players) {
			if (player.playerStatus == PlayerStatus.NONE || player.isBot())
			{
				if(player.isBot() && player.isActive)
					nActNum++;
				continue;
			}
			if(player.isActive)
				nActNum++;
			Player showPlayer = (table.showPlayer == null) ? player : table.showPlayer;
			Hand hand = new Hand(table.board);
			hand.addCards(showPlayer.getCards());
			HandValue handValue = new HandValue(hand);
			List<Card> cards = handValue.getBestCards();

			ISFSObject resObj = new SFSObject();
			resObj.putInt("pos", showPlayer.getPos());
			resObj.putBool("winner", false);
			resObj.putInt("value", handValue.getType().getValue());
			resObj.putUtfString("description", handValue.getDescription());
			List<Integer> cardArray = new ArrayList<Integer>();
			for (Card card : cards)
				cardArray.add(card.hashCode());
			resObj.putIntArray("cards", cardArray);
			resObj.putInt("ActNum", nActNum);
			if (getParentRoom().getUserByName(player.getEmail()) != null)
				send("texas_showbestcards", resObj, getParentRoom().getUserByName(player.getEmail()));
		}
		//for log trace
	 	LogOutput.traceLog("[showBestCards] ends");
	 	//trace("[showBestCards] ends");
	}
	
	public void dealerShareToDealer(long dealerShare)
	{
		//for log trace
	 	LogOutput.traceLog("[dealerShareToDealer] begins");
	 	//trace("[dealerShareToDealer] begins");
		ISFSObject resObj = new SFSObject();
		resObj.putLong("value", dealerShare);
		send("texas_dealerShareToDealer", resObj, getParentRoom().getUserList());
		//for log trace
	 	LogOutput.traceLog("[dealerShareToDealer] ends");
	 	//trace("[dealerShareToDealer] ends");
	}
	
	public void hideBestCards() {
		//for log trace
	 	LogOutput.traceLog("[hideBestCards] begins");
	 	//trace("[hideBestCards] begins");
		if (getParentRoom().getUserList().size() > 0)
			send("texas_hidebestcards", new SFSObject(), getParentRoom().getUserList());
		//for log trace
	 	LogOutput.traceLog("[hideBestCards] ends");
	 	//trace("[hideBestCards] ends");
	}

	public void showWinnerCards(Player player) {
		//for log trace
	 	LogOutput.traceLog("[showWinnerCards] begins");
	 	//trace("[showWinnerCards] begins");
		Card[] cards = player.getCards();
		ISFSObject obj = new SFSObject();
		obj.putInt("pos", player.getPos());
		obj.putInt("card1", cards[0].hashCode());
		obj.putInt("card2", cards[1].hashCode());
		obj.putInt("ActNum", nActNum);
		if (getParentRoom().getUserList().size() > 0)
			send("texas_show_winner_cards", obj, getParentRoom().getUserList());
		//for log trace
	 	LogOutput.traceLog("[showWinnerCards] ends");
	 	//trace("[showWinnerCards] ends");
	}
	
	public void payWinnerChips(Player player, Long pot, HandValue handValue, Long remainPot)
	{
		//for log trace
	 	LogOutput.traceLog("[payWinnerChips] begins");
	 	//trace("[payWinnerChips] begins");
		Card[] cards = player.getCards();
		ISFSObject obj = new SFSObject();
		obj.putInt("pos", player.getPos());
		obj.putLong("chip", player.getCash().longValue());
		obj.putLong("pot", pot);
		obj.putLong("remain_pot", remainPot);
		obj.putInt("card1", cards[0].hashCode());
		obj.putInt("card2", cards[1].hashCode());
		obj.putUtfString("hand", handValue.getDescription());
		if(!player.isBot()) {
			long total_earning = getPlayerTotalEarning(player.getEmail());
			int lv = getPlayerLevel(player.getEmail());
			total_earning += pot;
			if(total_earning >= Globals.levelPoints[lv])
			{
				total_earning = 0;
				UpdateLevel(player.getEmail(), lv + 1);
			}

			setPlayerTotalEarning(player.getEmail(), total_earning);
			obj.putLong("total_earning", total_earning);
		}

		if (getParentRoom().getUserList().size() > 0)
			send("texas_pay_winner_chips", obj, getParentRoom().getUserList());
		//for log trace
	 	LogOutput.traceLog("[payWinnerChips] ends");
	 	//trace("[payWinnerChips] ends");
	}
	
	public void UpdateLevel(String email, int lv)
	{
		//for log trace
	 	LogOutput.traceLog("[UpdateLevel] begins");
	 	//trace("[UpdateLevel] begins");
		IDBManager dbManager = getParentZone().getDBManager();
		String sql = "UPDATE user"
				+ " SET level=" + lv
				+ " WHERE email=\"" + email + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		
		sql = "INSERT INTO news_level(email, level, time)"
				+ " VALUES(\"" + email + "\""
				+ "," + lv
				+ "," + System.currentTimeMillis() + ")";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		
		ISFSObject obj = new SFSObject();
		obj.putInt("level", lv);
		if(getParentRoom().getUserByName(email) != null)
			send("update_level", obj, getParentRoom().getUserByName(email));
		//for log trace
	 	LogOutput.traceLog("[UpdateLevel] ends");
	 	//trace("[UpdateLevel] ends");
	}
	
	public void hideWinners() {
		//for log trace
	 	LogOutput.traceLog("[hideWinners] begins");
	 	//trace("[hideWinners] begins");
		if (getParentRoom().getUserList().size() > 0)
			send("texas_hide_winners", new SFSObject(), getParentRoom().getUserList());
		//for log trace
	 	LogOutput.traceLog("[hideWinners] ends");
	 	//trace("[hideWinners] ends");
	}

	public void updateHandHistory(String name, long pot, HandValue handValue) {
		//for log trace
	 	LogOutput.traceLog("[updateHandHistory] begins");
	 	//trace("[updateHandHistory] begins");

		sendMessage("", "Dealer", name + " wins " + table.getGameText(pot) + " with " + handValue.toString(), true);

		ISFSObject obj = new SFSObject();
		obj.putUtfString("name", name);
		obj.putLong("pot", pot);
		obj.putUtfString("hand", handValue.getWholeCardString());
		if (getParentRoom().getUserList().size() > 0)
			send("texas_update_hand_history", obj, getParentRoom().getUserList());
		//for log trace
	 	LogOutput.traceLog("[updateHandHistory] ends");
	 	//trace("[updateHandHistory] ends");
	}

	@Override
	public void playerActed(Player playerToNotify, Player player) {
		Action action = player.getAction();
		// System.out.println(action.getName());
		if (action != null) {
			if (player.getClient() != this) {
				waitForUserInput();
			}
		}
	}

	public void sendAllowedActions(Player playerToNotify, BigDecimal minBet, Set<Action> allowedActions) {
		if (getParentRoom().getUserByName(playerToNotify.getEmail()) == null)
			return;
		// Send client actions
		ISFSObject resObj = new SFSObject();
		resObj.putLong("pot", playerToNotify.getBet().longValue());
		resObj.putLong("min_bet", minBet.longValue());
		resObj.putLong("chip", playerToNotify.getCash().longValue());
		resObj.putInt("action_size", allowedActions.size());
		int i = 0;
		for (Action action : allowedActions) {
			resObj.putUtfString("action" + i, action.getName());
			i++;
		}
		send("texas_allowedactions", resObj, getParentRoom().getUserByName(playerToNotify.getEmail()));
	}

	@Override
	public Action act(Player playerToNotify, BigDecimal bigBlind, BigDecimal callAmount, BigDecimal minBetAmount,
			BigDecimal maxBetAmount, List<Card> board, Set<Action> allowedActions) {
		return getUserInput(playerToNotify, callAmount, minBetAmount, maxBetAmount, allowedActions);
	}

	/**
	 * Sets whether the actor is in turn.
	 * 
	 * @param isInTurn
	 *            Whether the actor is in turn.
	 */
	private void setActorInTurn(Player playerToNotify, int _pos, boolean isInTurn) {
		if (getParentRoom().getUserByName(playerToNotify.getEmail()) == null)
			return;
		ISFSObject resObj = new SFSObject();
		resObj.putInt("actor_pos", _pos);
		resObj.putBool("show", isInTurn);
		resObj.putInt("waiting_time", isFast ? 10 : 20);
		send("texas_setactor", resObj, getParentRoom().getUserByName(playerToNotify.getEmail()));
	}

	/**
	 * Sets the dealer.
	 * 
	 * @param isDealer
	 *            Whether the player is the dealer.
	 */
	private void setDealer(Player playerToNotify, int _pos, boolean isDealer) {
		if (getParentRoom().getUserByName(playerToNotify.getEmail()) == null)
			return;
		ISFSObject resObj = new SFSObject();
		resObj.putInt("dealer_pos", _pos);
		resObj.putBool("is_dealer", isDealer);
		send("texas_setdealer", resObj, getParentRoom().getUserByName(playerToNotify.getEmail()));
	}

	// public void dealCards(int round)
	// {
	// ISFSObject resObj = new SFSObject();
	// resObj.putInt("round", round);
	// if(getParentRoom().getUserList() != null)
	// send("texas_dealcards", resObj, getParentRoom().getUserList());
	// }

	Room getGameRoom() {
		return this.getParentRoom();
	}

	void setSelectedAction(int mode, long value) {
		if (mode == 1)
			selectedAction = Action.FOLD;
		else if (mode == 2)
			selectedAction = Action.CHECK;
		else if (mode == 3)
			selectedAction = Action.CALL;
		else if (mode == 4)
			selectedAction = new RaiseAction(new BigDecimal(value));
		else if (mode == 5)
			selectedAction = Action.ALL_IN;

		timer.cancel();
		synchronized (monitor) {
			monitor.notifyAll();
		}
	}

	/**
	 * Waits for the user to click the Continue button.
	 */
	public void waitForUserInput() {
		selectedAction = null;
		// SetTimer
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// Your database code here
				if (selectedAction == null) {
					selectedAction = Action.CONTINUE;
					synchronized (monitor) {
						monitor.notifyAll();
					}
				}
			}
		}, 1 * 1000);

		while (selectedAction == null) {
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

	/**
	 * Waits for the user to click an action button and returns the selected action.
	 * 
	 * @param minBet
	 *            The minimum bet.
	 * @param cash
	 *            The player's remaining cash.
	 * @param allowedActions
	 *            The allowed actions.
	 * 
	 * @return The selected action.
	 */
	public Action getUserInput(Player playerToNotify, BigDecimal callAmount, BigDecimal minBetAmount,
			BigDecimal maxBetAmount, final Set<Action> allowedActions) {
		selectedAction = null;

		if (getParentRoom().getUserByName(playerToNotify.getEmail()) == null)
			return null;

		// Send client actions
		ISFSObject resObj = new SFSObject();
		resObj.putLong("pot", playerToNotify.getBet().longValue());
		resObj.putLong("call_amount", callAmount.longValue());
		resObj.putLong("min_bet", minBetAmount.longValue());
		resObj.putLong("max_bet", maxBetAmount.longValue());
		resObj.putInt("action_size", allowedActions.size());
		resObj.putLong("cash", playerToNotify.getCash().longValue());
		int i = 0;
		for (Action action : allowedActions) {
			resObj.putUtfString("action" + i, action.getName());
			i++;
		}
		if(!playerToNotify.isAllIn())      
		{
			send("texas_userinput", resObj, getParentRoom().getUserByName(playerToNotify.getEmail()));
	
			timer = new Timer();
			task = new TimerTask() {
				@Override
				public void run() {
					// Your database code here
					if (selectedAction == null) {
						selectedAction = Action.NO_RESPONSE;
						synchronized (monitor) {
							monitor.notifyAll();
						}
					}
				}
			};
			timer.schedule(task, ((isFast) ? 20 : 20) * 1000);
	
			while (selectedAction == null) {
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
			if (getParentRoom().getUserByName(playerToNotify.getEmail()) != null)
				send("texas_hidebuttons", new SFSObject(), getParentRoom().getUserByName(playerToNotify.getEmail()));
		}
		return selectedAction;
	}

	public void potAnim(int pos) {
		//for log trace
	 	LogOutput.traceLog("[potAnim] begins");
	 	//trace("[potAnim] begins");
		if (getParentRoom().getUserList().size() > 0) {
			ISFSObject obj = new SFSObject();
			obj.putInt("pos", pos);
			send("texas_potanim", obj, getParentRoom().getUserList());
		}
		//for log trace
	 	LogOutput.traceLog("[potAnim] ends");
	 	//trace("[potAnim] ends");
	}

	public void kickPlayers() {
		if (getParentRoom().getUserList().size() > 0)
			send("texas_kick", new SFSObject(), getParentRoom().getUserList());
		getApi().removeRoom(getParentRoom());
	}

	public void setGift(ISFSObject params) {
		String email = params.getUtfString("email");
		boolean isAll = params.getBool("all");
		int pos = params.getInt("to");
		int category = params.getInt("category");
		int detail = params.getInt("detail");
		long amount = params.getLong("amount");
		boolean isCoin = params.getBool("coin");
		long chip = getPlayerChipcount(email);
		long coin = getPlayerCoin(email);
		if (isCoin)
			payPlayerCoin(email, amount);
		else
			payPlayerChipcount(email, amount);
		boolean isMoney = false;
		for (Player player : table.players) {
			if (player.playerStatus == PlayerStatus.NONE)
				continue;
			if((!isCoin && (chip >= amount)) || (isCoin && (coin >= amount))){
				if (isAll || (pos == player.getPos())) {
					player.setGift(category, detail);
					if (!player.isBot())
						setGift(player.getEmail(), category, detail);
				}
				isMoney = true;
			}
		}
		params.putBool("isMoney", isMoney);
		params.putLong("chip",chip);
		params.putLong("amount", amount);
		params.putLong("Coinamount", coin);
		// for(Player player : table.newPlayers) {
		// if(isAll || (pos == player.getPos())) {
		// player.giftCategory = category;
		// player.giftDetail = detail;
		// }
		// }
		// updateAll();
		if (getParentRoom().getUserList().size() > 0)
			send("texas_gift", params, getParentRoom().getUserList());
	}

	public void ShowAnimEmoji(ISFSObject params) {
		if (getParentRoom().getUserList().size() > 0)
			send("texas_anim_emoji", params, getParentRoom().getUserList());
	}

	public void addPlayerChipcount(String email, long value) {
		long chip = getPlayerChipcount(email);
		//update_daily_hand(email, value);		
		setPlayerChipcount(email, chip + value);		
	}

	public void payPlayerChipcount(String email, long value) {
		//for log trace
	 	LogOutput.traceLog("[payPlayerChipcount] begins");
	 	//trace("[payPlayerChipcount] begins");
	 	
		long chip = getPlayerChipcount(email);
		/*if(chip >= value)
			update_daily_hand(email, -value);
		else
			update_daily_hand(email, -chip);
		*/
		if(chip >= value){
			chip -= value;
		//if (chip < 0)
		//	chip = 0;
			setPlayerChipcount(email, chip);
		}
		//for log trace
	 	LogOutput.traceLog("[payPlayerChipcount] ends");
	 	//trace("[payPlayerChipcount] ends");
	}

	public void update_daily_hand(String email, long value) {
		if(value <= 0)
			return;
		
		long curTime = System.currentTimeMillis();
		Date curDate = new Date(curTime);
		Date endDate = new Date(curDate.getYear(), curDate.getMonth(), curDate.getDate(), 14, 0, 0); //2:00 PM
		if(curDate.after(endDate))
			curDate.setDate(curDate.getDate() + 1);
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String dateStr = dateFormatGmt.format(curDate);
		
		dailyHandError = "";
		long curGain = getPlayerDateGain(email, dateStr);
		if(curGain == 0) {
			if(dailyHandError == "Not Found") // not exist
				insert_daily_gain(email, dateStr, value);
			else if(dailyHandError == "") // exist, but gain is 0
				update_daily_gain(email, dateStr, value);
		}
		else {
			update_daily_gain(email, dateStr, value);			
		}
	}
public void update_daily_gain(String email, String dateStr, long value) {
	IDBManager dbManager = getParentZone().getDBManager();
	
	String sql = "UPDATE daily_hand SET gain = CASE WHEN (gain < "
			+ value + ")THEN " + value + " ELSE gain END" + " WHERE email=\"" + email
			 + "\" AND gain_date=\"" + dateStr
			 + "\"";

	try {
		dbManager.executeUpdate(sql, new Object[] {});
	} catch (SQLException e) {
		trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
	}					
}
	
	public void insert_daily_gain(String email, String dateStr, long value) {
		IDBManager dbManager = getParentZone().getDBManager();

		String sql = "INSERT INTO daily_hand(email, gain_date, gain)"
				+ " VALUES (\""
				+ email + "\",\""
				+ dateStr + "\",\""
				+ value + "\")";
        try {
            dbManager.executeUpdate(sql, new Object[] {});
        }
        catch (SQLException e) {
            trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
        }				
	}
	
public long getPlayerDateGain(String email, String dateStr) {
	long gain = 0;
	IDBManager dbManager = getParentZone().getDBManager();
	// System.out.println(email);
	String sql = "SELECT gain FROM daily_hand WHERE email=\"" + email
			 + "\" AND gain_date=\"" + dateStr
			 + "\"";
	try {
		ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
		if(res.size() > 0) {
			gain = res.getSFSObject(0).getLong("gain");
		}
		else {
			gain = 0;
			dailyHandError = "Not Found";
		}
	} catch (SQLException e) {
		trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		gain = 0;
		dailyHandError = "SQL Failed";
	}
	return gain;		
}

	public void setPlayerChipcount(String email, long value) {
		IDBManager dbManager = getParentZone().getDBManager();
		String sql = "UPDATE user SET chip=" + value + " WHERE email=\"" + email + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}

	public long getPlayerChipcount(String email) {
		long chipcount = 0;
		IDBManager dbManager = getParentZone().getDBManager();
		// System.out.println(email);
		String sql = "SELECT chip FROM user WHERE email=\"" + email + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			if (res.size() > 0)
				chipcount = res.getSFSObject(0).getLong("chip");
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return chipcount;
	}

	public void payPlayerCoin(String email, long value) {
		long coin = getPlayerCoin(email);
		if(coin >= value){
			coin -= value;
		//if (coin < 0)
		//	coin = 0;
			setPlayerCoin(email, coin);
		}
	}

	public void setPlayerCoin(String email, long value) {
		IDBManager dbManager = getParentZone().getDBManager();
		String sql = "UPDATE user SET coin=" + value + " WHERE email=\"" + email + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}

	public long getPlayerCoin(String email) {
		long coin = 0;
		IDBManager dbManager = getParentZone().getDBManager();
		// System.out.println(email);
		String sql = "SELECT coin FROM user WHERE email=\"" + email + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			coin = res.getSFSObject(0).getLong("coin");
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return coin;
	}

	public ISFSObject getUserInfo(String email) {
		IDBManager dbManager = getParentZone().getDBManager();
		String sql = "SELECT * FROM user WHERE email=\"" + email + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			return res.getSFSObject(0);
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return null;
	}

	public void setGift(String email, int giftCategory, int giftValue) {
		IDBManager dbManager = getParentZone().getDBManager();
		String sql = "UPDATE user" + " SET gift_category=" + giftCategory + ", gift_value=" + giftValue
				+ " WHERE email=\"" + email + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}

	public void addPlayInfo(String email, long winAmount) {
		IDBManager dbManager = getParentZone().getDBManager();
		String sql = "UPDATE user" + " SET games_played=games_played+1" + ", biggest_win = CASE WHEN (biggest_win < "
				+ winAmount + ") THEN " + winAmount + " ELSE biggest_win END" + " WHERE email=\"" + email + "\"";
		// System.out.println(sql);
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
	}
	
	public void addPlayerTotalEarning(String email, long value) {
		setPlayerTotalEarning(email, value + getPlayerTotalEarning(email));
	}
	
	public void setPlayerTotalEarning(String email, long value) {
		//for log trace
	 	LogOutput.traceLog("[setPlayerTotalEarning] begins");
	 	//trace("[setPlayerTotalEarning] begins");
		IDBManager dbManager = getParentZone().getDBManager();
		String sql = "UPDATE user SET total_earning=" + value + " WHERE email=\"" + email + "\"";
		try {
			dbManager.executeUpdate(sql, new Object[] {});
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		//for log trace
	 	LogOutput.traceLog("[setPlayerTotalEarning] ends");
	 	//trace("[setPlayerTotalEarning] ends");
	}

	public long getPlayerTotalEarning(String email) {
		long total_earning = 0;
		IDBManager dbManager = getParentZone().getDBManager();
		// System.out.println(email);
		String sql = "SELECT total_earning FROM user WHERE email=\"" + email + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			if (res.size() > 0)
				total_earning = res.getSFSObject(0).getLong("total_earning");
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return total_earning;
	}
	
	public int getPlayerLevel(String email) {
		int lv = 1;
		IDBManager dbManager = getParentZone().getDBManager();
		// System.out.println(email);
		String sql = "SELECT level FROM user WHERE email=\"" + email + "\"";
		try {
			ISFSArray res = dbManager.executeQuery(sql, new Object[] {});
			if (res.size() > 0)
				lv = res.getSFSObject(0).getInt("level");
		} catch (SQLException e) {
			trace(ExtensionLogLevel.WARN, "SQL Failed: " + e.toString());
		}
		return lv;
	}
	
	public boolean isPrivate()
	{
		return dynamicRoomType == DynamicRoomType.RT_PRIVATE;

	}
	public boolean isDefault()
	{
		return dynamicRoomType == DynamicRoomType.RT_DEFAULT;

	}
	
	public boolean canAutoRebuy(Player player)
	{
		long amount = getPlayerChipcount(player.getEmail());
		long minBuyin = TEXAS_INFO[blindType].minBuyin;
		return amount >= minBuyin;
	}

	public static void createTexasDefaultRooms(Zone zone)
	{
		int blindTypeCount = TEXAS_INFO.length;
		int seatTypeCount = 2;
		int[][] roomCounts = new int[blindTypeCount][seatTypeCount];
		ZoneExtension.mutex.lock();
		try
		{
			List<Room> roomList = zone.getRoomList();
			for(Room room : roomList) {
				
				if(!room.isGame() || room.getGroupId().compareTo("TexasPoker") != 0)
					continue;
				int tableSize = 9;
				RoomVariable v = room.getVariable("table_size");
				if(v == null)
					continue;
				tableSize = v.getIntValue();
				int blindType = 0;
				v = room.getVariable("blind_type");
				if(v == null)
					continue;
				blindType = v.getIntValue();
				int seatIndex = tableSize == 5 ? 0 : 1;
				if(blindType < blindTypeCount)
					roomCounts[blindType][seatIndex]++;
			}
			for(int i = 0; i < blindTypeCount; i++)
			{
				for(int j = 0; j < seatTypeCount; j++)
				{
					int seat = j == 0 ? 5 : 9;
					boolean isFast = true;
					while(roomCounts[i][j] < 2)
					{
						boolean isEmpty = (roomCounts[i][j] % 2) != 0;
						CreateTexasRoom(zone, i, seat, isFast, isEmpty, DynamicRoomType.RT_DEFAULT, null);
						roomCounts[i][j]++;
					}
				}
			}			
		}
		finally
		{
			ZoneExtension.mutex.unlock();
		}

	}
	
	private static List<Room> getEmptyTexasRoomList(Zone zone, int blindType, int seat, boolean speed, boolean isEmpty)
	{
		List<Room> roomList = zone.getRoomList();
		List<Room> emptyRoomList = new ArrayList<Room>();
		
		for(Room room : roomList) {
			if(!room.isGame() || room.getGroupId().compareTo("TexasPoker") != 0)
				continue;
			
			int userCount = room.getUserList().size();
			int tableSize = 9;
			RoomVariable v = room.getVariable("table_size");
			if(v == null)
				continue;
			tableSize = v.getIntValue();
			if(tableSize != seat)
				continue;
			int curBlindType = 0;
			v = room.getVariable("blind_type");
			if(v == null)
				continue;
			curBlindType = v.getIntValue();
			if(blindType != curBlindType)
				continue;
			boolean curIsEmpty = false;
			v = room.getVariable("empty");
			if(v != null)
				curIsEmpty = v.getBoolValue();
			if(curIsEmpty != isEmpty)
				continue;
			if(userCount == 0)
				emptyRoomList.add(room);
		}
		return emptyRoomList;
	}
	
	public static void autoCreateTexasRooms(Zone zone, int blindType, int seat, boolean speed, boolean isEmpty)
	{
		ZoneExtension.mutex.lock();
		try
		{
			List<Room> createdRoomList = new ArrayList<Room>();
			List<Room> emptyRoomList = getEmptyTexasRoomList(zone, blindType, seat, speed, isEmpty);
			int emptyRoomCount = emptyRoomList.size();
			while(emptyRoomCount < 1)
			{
				Room room = CreateTexasRoom(zone, blindType, seat, speed, isEmpty, DynamicRoomType.RT_AUTO_CREATE, null);
				if(room != null)
					createdRoomList.add(room);
				emptyRoomCount++;
			}
			if(emptyRoomList.size() < 1)
			{
				long blind = getBlind(blindType);
				updateRoomList(zone, blind, null);
			}
		}
		finally
		{
			ZoneExtension.mutex.unlock();
		}
	}

	public static void autoDeleteTexasRooms(Zone zone, int blindType, int seat, boolean speed, boolean isEmpty)
	{
		ZoneExtension.mutex.lock();
		try
		{
			ISFSArray res = new SFSArray();
			List<Room> deletedRoomList = new ArrayList<Room>();
			List<Room> emptyRoomList = getEmptyTexasRoomList(zone, blindType, seat, speed, isEmpty);
			for(int i = emptyRoomList.size() - 1; i >= 1  ; i--)
			{
				if(!emptyRoomList.get(i).isDynamic())
					continue;
				((BaseSFSExtension)zone.getExtension()).getApi().removeRoom(emptyRoomList.get(i));
				deletedRoomList.add(emptyRoomList.get(i));
				ISFSObject obj = new SFSObject();
				obj.putInt("table_pos", i);
				res.addSFSObject(obj);
			}
			if(deletedRoomList.size() > 0)
			{
				long blind = getBlind(blindType);
				updateRoomList(zone, blind, null);
				ISFSObject response = new SFSObject();
				List<User> userList = new ArrayList<User>();
				userList.addAll(zone.getUserList());
				zone.getExtension().send("update_friend_room", response, userList);
			}
		}
		finally
		{
			ZoneExtension.mutex.unlock();
		}
	}
	
	public static void updateRoomList(Zone zone, long blind, User user)
	{
		ZoneExtension.mutex.lock();
		try
		{
			ISFSArray res = getTexasRoomList(zone, blind);
			
			ISFSObject response = new SFSObject();	
			response.putSFSArray("array", res);
			response.putInt("game_type", 0);
			response.putLong("blind", blind);
			if(user != null)
				zone.getExtension().send("get_room_list", response, user);
			else
			{
				List<User> userList = new ArrayList<User>();
				userList.addAll(zone.getUserList());
				zone.getExtension().send("update_room_list", response, userList);
			}			
		}
		finally
		{
			ZoneExtension.mutex.unlock();
		}
	}	
	
	public static ISFSArray getTexasRoomList(Zone zone, long blind)
	{
		ISFSArray res = new SFSArray();
		List<Room> roomList = zone.getRoomList();
		List<ISFSObject> tempList = new ArrayList<ISFSObject>();
		for(Room room : roomList)
		{
			if(!room.isGame() || room.getGroupId().compareTo("TexasPoker") != 0)
				continue;
			ISFSObject obj = (ISFSObject) room.getExtension().handleInternalMessage("get_room_info", null);
			if(obj != null)
			{
				if(blind >= 10000000 && obj.getLong("blind") >= 10000000)
					tempList.add(obj);
				else if(blind == obj.getLong("blind"))
					tempList.add(obj);
			}
		}
		
		// sort room list by min_buyin and is_empty
		tempList.sort(new Comparator<ISFSObject>() {
			@Override
			public int compare(ISFSObject o1, ISFSObject o2) {
				boolean empty1 = o1.getBool("is_empty");
				boolean empty2 = o2.getBool("is_empty");
				long min1 = o1.getLong("min_buyin");
				long min2 = o2.getLong("min_buyin");
				long tableSize1 = o1.getInt("size");
				long tableSize2 = o2.getInt("size");
				long playerNum1 = o1.getInt("player_num");
				long playerNum2 = o2.getInt("player_num");
				long botPlayerNum1 = o1.getInt("bot_player_num");
				long botPlayerNum2 = o2.getInt("bot_player_num");
				
				
				if(empty1 != empty2)
				{
					if(empty1 == true)
						return 1;
					else
						return -1;
				}
				else
				{
					if(min1 > min2)
						return 1;
					else if(min1 < min2)
						return -1;
					else
					{
						if(tableSize1 > tableSize2)
							return 1;
						else if(tableSize1 < tableSize2)
							return -1;
						else
						{
							if(botPlayerNum1 < botPlayerNum2)
								return 1;
							else if(botPlayerNum1 > botPlayerNum2)
								return -1;
							else
								return 0;
						}
					}
				}
			}
		});
		
		res = new SFSArray();
		for(int i = 0; i < tempList.size(); i++)
		{
			res.addSFSObject(tempList.get(i));
		}
		return res;
	}
	
	public static Room CreateTexasRoom(Zone zone, int blindType, int seat, boolean speed, boolean isEmpty, DynamicRoomType dynamicRoomType, String roomName)
	{
		String groupName =  "TexasPoker";
		String tableName = roomName != null ? roomName : ZoneExtension.GetNewRoomName(zone, blindType, seat, isEmpty, groupName); 
		
		CreateRoomSettings settings = new CreateRoomSettings();
		settings.setGame(true);
		settings.setName(tableName);
		settings.setGroupId(groupName);
		settings.setDynamic(true);
		settings.setMaxUsers(20);
		List<RoomVariable> roomVariables = new ArrayList<RoomVariable>();
		SFSRoomVariable rv = new SFSRoomVariable("table_size", seat);
		roomVariables.add(rv);
		rv = new SFSRoomVariable("blind_type", blindType);
		roomVariables.add(rv);
		rv = new SFSRoomVariable("speed", speed);
		roomVariables.add(rv);
		String tableType = dynamicRoomType.toString();
		rv = new SFSRoomVariable("dynamic_table_type", tableType);
		roomVariables.add(rv);
		rv = new SFSRoomVariable("empty", isEmpty);
		roomVariables.add(rv);		
		settings.setRoomVariables(roomVariables);
		settings.setAutoRemoveMode(SFSRoomRemoveMode.NEVER_REMOVE);
		
		RoomExtensionSettings extension = null;
		extension = new RoomExtensionSettings("Pokerat", "TexasPokerExtension.RoomExtension");
		settings.setExtension(extension);
		
		try {
			return ((BaseSFSExtension)zone.getExtension()).getApi().createRoom(zone, settings, null);
	} catch (SFSCreateRoomException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}		
}
