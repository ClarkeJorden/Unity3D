package TexasPokerEngine;

import TexasPokerExtension.RoomExtension;

import TexasPokerAction.Action;
import TexasPokerAction.BetAction;
import TexasPokerAction.RaiseAction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.Timer;
import java.util.TreeMap;

import TexasPokerEngine.LogOutput;

public class Table {

	private static final int MAX_RAISES = 3;
	private static final boolean ALWAYS_CALL_SHOWDOWN = false;

	private final RoomExtension gameExt;

	public final PlayMode playMode;
	private final TableType tableType;
	private final BigDecimal bigBlind;
	private int tableSize;

	public Player[] players;

	public boolean isRunning;

	public final Deck deck;
	public final List<Card> board;
	private final List<Pot> pots;

	public int dealerPosition;
	public Player dealer;
	public int actorPosition;
	public Player actor = null;
	public BigDecimal minBet, bet, minBetDiff;
	public Player lastBettor;
	public int raises;
	public Player showPlayer = null;

	public int dealerPos;
	public int smallBlindPos;
	public int bigBlindPos;
	public boolean isSelect = false;
	public int playersToAct;

	private Timer timer;
	private boolean delayFlag;
	private final Object monitor = new Object();

	public String whereis() {
		StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
		String where = this.gameExt.roomName + ": " + ste.getClassName() + " " + ste.getMethodName() + " " + ste.getLineNumber() + " ";
//		System.out.println(where);
		return where;
	}


	public Table(PlayMode mode, TableType type, int tableSize, BigDecimal bigBlind, RoomExtension _client) {
		this.playMode = mode;
		this.tableType = type;
		this.tableSize = tableSize;
		this.bigBlind = bigBlind;
		this.gameExt = _client;

		players = new Player[tableSize];
		for (int i = 0; i < tableSize; i++)
			players[i] = new Player();

		deck = new Deck();
		board = new ArrayList<>();
		pots = new ArrayList<>();
		timer = new Timer();
		this.isRunning = false;
	}

	// public void addPlayer(Player player) {
	// newPlayers.add(player);
	// }

	public boolean isRunning() {
		return isRunning;
	}
	
	public boolean isProtable()
	{
		return bigBlind.longValue() >= 10000000L;
	}

	public int playerSize() {
		int count = 0;
		for (Player player : players) {
			if (player.playerStatus != PlayerStatus.NONE)
				count++;
		}
		return count;
	}

	public int activePlayerSize() {
		int count = 0;
		for (Player player : players) {
			if (player.playerStatus == PlayerStatus.ACTIVE && player.isActive)
				count++;
		}
		return count;
	}

	public int botPlayerSize() {
		int count = 0;
		for (Player player : players) {
			if (player.playerStatus != PlayerStatus.NONE && player.isBot())
				count++;
		}
		return count;
	}

	public BigDecimal getTotalChip() {
		BigDecimal chip = BigDecimal.valueOf(0);
		for (Player player : players) {
			if (player.playerStatus != PlayerStatus.NONE) {
				chip.add(player.getCash());
			}
		}
		chip.add(getTotalPot());
		return chip;
	}

	public String getGameText(long chip) {
		String str = "$";
		if (chip < 1000)
			str += chip;
		else if (chip < 1000000)
			str += String.format("%.1fK", (double) (chip / 100) / 10);
		else if (chip < 1000000000)
			str += String.format("%.1fM", (double) (chip / 100000) / 10);
		else if (chip < 1000000000000l)
			str += String.format("%.1fB", (double) (chip / 100000000) / 10);
		else
			str += String.format("%.1fT", (double) (chip / 100000000000l) / 10);
		return str;
	}

	public void run() {
		// for debug by jbj 20180904
	 	this.whereis();
	 	//for log trace
	 	LogOutput.traceLog("[run] begins");
		////////////////////////////
		isRunning = true;
		dealerPosition = -1;
		actorPosition = -1;
		while (true) {
			for (Player player : players) {
				// for debug by jbj 20180904
			 	this.whereis();
				////////////////////////////

				if (player.playerStatus == PlayerStatus.NONE)
					continue;
				if (player.isAddChip) {
					//for log trace
				 	LogOutput.traceLog("case : player.isAddChip is true");
					gameExt.sendNewChip(player);
				}
				if (player.getCash().longValue() == 0) {
					//for log trace
				 	LogOutput.traceLog("case : player.getCash().longValue() is 0");
					if (player.isAutoRebuy && gameExt.canAutoRebuy(player)) {
						//for log trace
					 	LogOutput.traceLog("case : player.isAutoRebuy is true");
						gameExt.sendNewChip(player);
					} else {
						//for log trace
					 	LogOutput.traceLog("case : player.isAutoRebuy is false");
						gameExt.sendRebuy(player);
					}
				}
			}
			
			// for debug by jbj 20180904
		 	this.whereis();
			////////////////////////////

			for (Player player : players) {
				if (player.playerStatus == PlayerStatus.NEW) {
					//for log trace
				 	LogOutput.traceLog("case : player.playerStatus is PlayerStatus.NEW");

					if (player.getCash().longValue() > 0) {
						//for log trace
					 	LogOutput.traceLog("case : player.getCash().longValue() > 0");
						player.setAction(Action.ANTE);
						player.playerStatus = PlayerStatus.ACTIVE;
					}
				}
			}

			// gameExt.resetButtons();
			// gameExt.updatePlayers(false, false);

			int noOfActivePlayers = 0;
			for (Player player : players) {
				if (player.playerStatus != PlayerStatus.ACTIVE)
					continue;
				if (player.getCash().compareTo(BigDecimal.ZERO) > 0) {
					//for log trace
				 	LogOutput.traceLog("case : player.getCash().compareTo(BigDecimal.ZERO) > 0");
					noOfActivePlayers++;
					if (player.getAction() != Action.ANTE)
					{
						//for log trace
					 	LogOutput.traceLog("case : player.getAction() is not Action.ANTE");
						player.setAction(null);
						
					}
				} else {
					//for log trace
				 	LogOutput.traceLog("case : player.getCash().compareTo(BigDecimal.ZERO) < 0");
					player.playerStatus = PlayerStatus.NEW;
					player.resetHand();
					// player.setAction(Action.BUSTED);
				}
			}
			if (noOfActivePlayers > 1) {
				//for log trace
			 	LogOutput.traceLog("case : noOfActivePlayers > 1");
				playHand();
			} else {
				break;
			}
		}
		
		
		// for debug by jbj 20180904
	 	this.whereis();
		////////////////////////////

		// Game over.
		board.clear();
		pots.clear();
		bet = BigDecimal.ZERO;
		notifyBoardUpdated();
		isRunning = false;
		for (Player player : players) {
			if (player.playerStatus == PlayerStatus.NONE)
				continue;
			player.resetHand();
		}
		notifyPlayersUpdated(false, false);
	 	//for log trace
	 	LogOutput.traceLog("[run] ends");
	}

	/**
	 * Plays a single hand.
	 */
	private void playHand() {
		//for log trace
	 	LogOutput.traceLog("[playHand] begins");
		// for debug by jbj 20180904
	 	this.whereis();
		////////////////////////////

		gameExt.sendMessage("", "Dealer", "A new round of the game has begun.", true);
		// for debug by jbj 20180904
		resetHand();
		rotateActor();
	 	this.whereis();
		////////////////////////////

		// Small blind.
		postSmallBlind();
		// for debug by jbj 20180904
	 	this.whereis();
		////////////////////////////

		// Big blind.
		rotateActor();
		postBigBlind();
		
		// for debug by jbj 20180904
	 	this.whereis();
		////////////////////////////
		// Pre-Flop.
		delayTimer(0.5f);
		dealHoleCards();
		doBettingRound(0);

		// Flop.
		if (activePlayerSize() > 1) {
			bet = BigDecimal.ZERO;
			// delayTimer(0.5f);
			minBetDiff = bigBlind;
			minBet = BigDecimal.ZERO;
			dealCommunityCards("Flop", 3);
			doBettingRound(1);

			// for debug by jbj 20180904
		 	this.whereis();
			////////////////////////////

			// Turn.
			if (activePlayerSize() > 1) {
				bet = BigDecimal.ZERO;
				// delayTimer(0.5f);
				dealCommunityCards("Turn", 1);
				minBetDiff = bigBlind;
				minBet = BigDecimal.ZERO;
				doBettingRound(2);

				// for debug by jbj 20180904
			 	this.whereis();
				////////////////////////////
				// River.
				if (activePlayerSize() > 1) {
					bet = BigDecimal.ZERO;
					// delayTimer(0.5f);
					minBetDiff = bigBlind;
					minBet = BigDecimal.ZERO;
					dealCommunityCards("River", 1);
					doBettingRound(3);

					// for debug by jbj 20180904
				 	this.whereis();
					////////////////////////////
					// Showdown.
					if (activePlayerSize() > 1) {
						bet = BigDecimal.ZERO;
						// for debug by jbj 20180904
					 	this.whereis();
						////////////////////////////
						doShowdown();
					}
				}
			}
		}
		
		// for debug by jbj 20180904
	 	this.whereis();
		////////////////////////////

		updatePlayInfo();
		delayTimer(1);
		//for log trace
	 	LogOutput.traceLog("[playHand] ends");
	}

	public int getNextActivePos(int pos) {
		for (int i = 1; i <= tableSize; i++) {
			int k = (i + pos) % tableSize;
			if (players[k].playerStatus == PlayerStatus.ACTIVE && players[k].isActive)
				return k;
		}
		return pos;
	}

	/**
	 * Resets the game for a new hand.
	 */
	private void resetHand() {
		//for log trace
	 	LogOutput.traceLog("[table->resetHand] begins");
		// Clear the board.
		board.clear();
		pots.clear();

		// Determine the active players.
		for (Player player : players) {
			if (player.playerStatus != PlayerStatus.ACTIVE)
				continue;
			player.resetHand();
			// Player must be able to afford at least the big blind.
			if (player.getCash().compareTo(BigDecimal.ZERO) > 0) {
				player.isActive = true;
			}
		}

		showPlayer = null;
		gameExt.hideBestCards();
		gameExt.updateBoard(4, false);

		// Rotate the dealer button.
		dealerPosition = getNextActivePos(dealerPosition);
		dealer = players[dealerPosition];
		// dealerPosition = (dealerPosition + 1) % activePlayerSize();
		// dealer = activePlayers.get(dealerPosition);

		// Shuffle the deck.
		deck.shuffle();

		// Determine the first player to act.
		actorPosition = dealerPosition;
		actor = players[actorPosition];
		// actorPosition = dealerPosition;
		// actor = activePlayers.get(actorPosition);

		// Set the initial bet to the big blind.
		minBet = bigBlind;
		bet = minBet;
		minBetDiff = bigBlind;

		// Notify all clients a new hand has started.
		// gameExt.setBlind(dealer, "(D)");
		// gameExt.setBlind(dealer, 0);
		//gameExt.setDealer(dealer.getPos());
		// dealer.setAction(Action.ANTE);
		dealerPos = dealer.getPos();
		// notifyPlayersUpdated(false);
		//for log trace
		LogOutput.traceLog("[table->resetHand] ends");
	}

	/**
	 * Rotates the position of the player in turn (the actor).
	 */
	private void rotateActor() {
		//for log trace
	 	LogOutput.traceLog("[rotateActor] begins");
		actorPosition = getNextActivePos(actorPosition);
		actor = players[actorPosition];
		// actorPosition = dealerPosition;
		// actor = activePlayers.get(actorPosition);
		//for log trace
	 	LogOutput.traceLog("[rotateActor] ends");
	}

	private void selectActor(boolean show) {
		//for log trace
	 	LogOutput.traceLog("[selectActor] begins");
		isSelect = show;
		gameExt.selectActor(actor, show, false);
		//for log trace
	 	LogOutput.traceLog("[selectActor] ends");
	}

	/**
	 * Posts the small blind.
	 */
	private void postSmallBlind() {
		//for log trace
	 	LogOutput.traceLog("[postSmallBlind] begins");
	 	
		final BigDecimal smallBlind = bigBlind.divide(BigDecimal.valueOf(2)); // TODO
		if (actor.getAction() == Action.ANTE) {
			actor.postBigBlind(bigBlind);
			contributePot(bigBlind);
		} else if (actor.getCash().compareTo(smallBlind) >= 0) {
			actor.postSmallBlind(smallBlind);
			contributePot(smallBlind);
		} else {
			actor.setAction(Action.ALL_IN);
			actor.postSmallBlind(actor.getCash());
			contributePot(actor.getCash());
		}
		// notifyBoardUpdated();
		smallBlindPos = actor.getPos();
		actor.setAction(Action.SMALL_BLIND);
		// actor.setBlind(1);
		// gameExt.setBlind(actor, "(SB)");
		// gameExt.setBlind(actor, 1);
		
		//for log trace
	 	LogOutput.traceLog("[postSmallBlind] ends");
	}

	/**
	 * Posts the big blind.
	 */
	private void postBigBlind() {
		//for log trace
	 	LogOutput.traceLog("[table->postBigBlind] begins");
		if (actor.getCash().compareTo(bigBlind) >= 0) {
			actor.postBigBlind(bigBlind);
			contributePot(bigBlind);
		} else {
			actor.setAction(Action.ALL_IN);
			actor.postBigBlind(actor.getCash());
			contributePot(actor.getCash());
		}
		// notifyBoardUpdated();
		bigBlindPos = actor.getPos();
		actor.setAction(Action.BIG_BLIND);

		for (Player player : players) {
			if (player.playerStatus == PlayerStatus.ACTIVE && player.isActive) {
				if (player.getAction() == Action.ANTE) {
					player.postBigBlind(bigBlind);
					contributePot(player, bigBlind);
				}
			}
		}

		// actor.setBlind(2);
		// gameExt.setBlind(actor, "(BB)");
		// gameExt.setBlind(actor, 2);
		gameExt.setBlind();
		//for log trace
	 	LogOutput.traceLog("[table->postBigBlind] ends");
	}

	/**
	 * Deals the Hole Cards.
	 */
	private void dealHoleCards() {
		//for log trace
	 	LogOutput.traceLog("[dealHoleCards] begins");
		int i = 0;
		for (Player player : players) {
			if (player.playerStatus == PlayerStatus.ACTIVE && player.isActive)
				player.setCards(deck.deal(2));
			i++;
		}
		// for (Player player : activePlayers) {
		// player.setCards(deck.deal(2));
		// }

		gameExt.dealCards();
		notifyPlayersUpdated(false, true);
		// gameExt.dealCards(0);
		// delayTimer(1);
		//for log trace
	 	LogOutput.traceLog("[dealHoleCards] ends");
	}

	/**
	 * Deals a number of community cards.
	 * 
	 * @param phaseName
	 *            The name of the phase.
	 * @param noOfCards
	 *            The number of cards to deal.
	 */
	private void dealCommunityCards(String phaseName, int noOfCards) {
		//for log trace
	 	LogOutput.traceLog("[dealCommunityCards] begins");
		for (int i = 0; i < noOfCards; i++) {
			board.add(deck.deal());
		}
		notifyPlayersUpdated(false, false);
		// delayTimer(1);
		//for log trace
	 	LogOutput.traceLog("[dealCommunityCards] ends");
	}

	/**
	 * Performs a betting round.
	 */
	private void doBettingRound(int round) {
		//for log trace
	 	LogOutput.traceLog("[doBettingRound] begins");
		// Determine the number of active players.
		playersToAct = 0;
		for (Player player : players) {
			if (player.playerStatus == PlayerStatus.ACTIVE && player.isActive) {
				if (!player.isAllIn())
					playersToAct++;
			}
		}
		// Determine the initial player and bet size.
		if (board.size() == 0) {
			// Pre-Flop; player left of big blind starts, bet is the big blind.
			bet = bigBlind;
		} else {
			// Otherwise, player left of dealer starts, no initial bet.
			actorPosition = dealerPosition;
			bet = BigDecimal.ZERO;
		}

		if (playersToAct == 2) {
			// Heads Up mode; player who is not the dealer starts.
			actorPosition = dealerPosition;
		}

		lastBettor = null;
		raises = 0;
		for (Player player : players) {
			if (player.playerStatus == PlayerStatus.ACTIVE && player.isActive)
			{
				player.raise = 0;
			}
		}
		gameExt.updateBoard(round, false);
		gameExt.showBestCards();
		delayTimer(0.5f);
		if (playersToAct > 1) {
			while (playersToAct > 0) {
				rotateActor();
				Action action;
				if (actor.isAllIn()) {
					// Player is all-in
					action = Action.ALL_IN;
					playersToAct--;
				} else {
					// Otherwise allow client to act.
					Set<Action> allowedActions = getAllowedActions(actor);
					selectActor(true);

					BigDecimal callAmount = minBet.subtract(actor.getBet());
					BigDecimal minBetAmount = callAmount.add(minBetDiff);
					BigDecimal maxBetAmount = getMaxBetAmount(actor);

					if (actor.isBot())
						action = actor.getClient().act(actor, bigBlind, minBet, minBetDiff, maxBetAmount, board,
								allowedActions);
//					else if(round == 0 && actor.getBet().longValue() == 0
//							&& actor.getCash().longValue() <= bigBlind.longValue())
//						action = Action.ALL_IN;
					else
						action = actor.getClient().act(actor, bigBlind, callAmount, minBetAmount, maxBetAmount, board,
								allowedActions);

					if (action == Action.RAISE && action.getAmount().compareTo(actor.getCash()) >= 0)
						action = Action.ALL_IN;
					if (action == Action.NO_RESPONSE || action == null) {
						if (allowedActions.contains(Action.ALL_IN))
							action = Action.ALL_IN;
						else if (allowedActions.contains(Action.CHECK))
							action = Action.CHECK;
						else
							action = Action.FOLD;
						// delayTimer(1);
					}
					selectActor(false);
					// Verify chosen action to guard against broken clients (accidental or on
					// purpose).
					if (!allowedActions.contains(action)) {
						if (action instanceof BetAction && !allowedActions.contains(Action.BET)) {
							throw new IllegalStateException(
									String.format("Player '%s' acted with illegal Bet action", actor));
						} else if (action instanceof RaiseAction && !allowedActions.contains(Action.RAISE)) {
							throw new IllegalStateException(
									String.format("Player '%s' acted with illegal Raise action", actor));
						}
					}
					playersToAct--;
					if (action == Action.CHECK) {
						// Do nothing.
					} else if (action == Action.CALL) {
						BigDecimal betIncrement = minBet.subtract(actor.getBet());
						if (betIncrement.compareTo(actor.getCash()) > 0) {
							betIncrement = actor.getCash();
						}
						actor.payCash(betIncrement);
						actor.setBet(actor.getBet().add(betIncrement));
						contributePot(betIncrement);
					} else if (action == Action.ALL_IN) {
						// System.out.println("mine: All-in");
						if (maxBetAmount.compareTo(actor.getCash()) < 0) {
							action = Action.RAISE;
						}
						bet = bet.add(maxBetAmount);
						actor.setBet(maxBetAmount.add(actor.getBet()));
						actor.payCash(maxBetAmount);
						contributePot(maxBetAmount);
						if (minBet.compareTo(actor.getBet()) < 0) {
							raises++;
							lastBettor = actor;
							playersToAct = activePlayerSize() - 1;
							minBetDiff = actor.getBet().subtract(minBet);
							minBet = actor.getBet();
						}
					} else if (action instanceof RaiseAction) {
						BigDecimal amount = action.getAmount();
						bet = bet.add(amount);
						actor.setBet(actor.getBet().add(amount));
						minBetDiff = actor.getBet().subtract(minBet);
						minBet = actor.getBet();
						actor.payCash(amount);
						contributePot(amount);
						lastBettor = actor;
						raises++;
						playersToAct = activePlayerSize() - 1;
					} else if (action == Action.FOLD) {
						// actor.setCards(null);
						actor.isActive = false;
						bet = bet.add(actor.getBet());
						contributePot(actor.getBet());
						if (activePlayerSize() == 1) {
							// board update due to update real pot and dealer pot
							// Only one player left, so he wins the entire pot.
							// notifyBoardUpdated();
							Player winner = new Player();
							for (Player player : players) {
								if (player.playerStatus == PlayerStatus.ACTIVE && player.isActive)
									winner = player;
							}
							BigDecimal amount = getTotalPot();
							float realPercentage = isProtable() ? 99.5f : 100;
							BigDecimal realAmount = amount.multiply(new BigDecimal(realPercentage)).divide(new BigDecimal(100));
							winner.win(realAmount);

							BigDecimal dealerShare = amount.subtract(realAmount);
							long dealerShareValue = dealerShare.longValue();

							// distribute a profit to dealer
							if(isProtable())
							{
								gameExt.showDealerPot(dealerShareValue);
								delayTimer(2.5f);
								gameExt.dealerShareToDealer(dealerShareValue);
								delayTimer(1.0f);
							}

							Hand hand = new Hand(board);
							hand.addCards(winner.getCards());
							HandValue handValue = new HandValue(hand);

							gameExt.updateHandHistory(winner.getName(), realAmount.longValue(), handValue);
							gameExt.payWinnerChips(winner, realAmount.longValue(), handValue, (long)0);

							
							showPlayer = winner;
							winner.isShow = true;
							winner.isWinner = true;
							gameExt.showBestCards();
							winner.isWinner = false;
							showPlayer = null;

							delayTimer(4);

							playersToAct = 0;

							return;
						}
					} else {
						// Programming error, should never happen.
						throw new IllegalStateException("Invalid action: " + action);
					}
				}
				if (actor.getCash().equals(BigDecimal.ZERO) && (action != Action.FOLD))
					action = Action.ALL_IN;
				actor.setAction(action);
				actor.raise = raises;
				for (Player player : players) {
					if (player.playerStatus == PlayerStatus.ACTIVE && player.isActive) {
						if (player == actor)
							player.raise = raises;
					}
				}
				gameExt.potAnim(actor.getPos());
				notifyBoardUpdated();
				notifyPlayersUpdated(false, false);
			}
		}

		delayTimer(0.5f);
		// Reset player's bets.
		for (Player player : players) {
			if (player.playerStatus == PlayerStatus.ACTIVE && player.isActive)
				player.resetBet();
		}
		notifyBoardUpdated();
		notifyPlayersUpdated(false, false);
		//for log trace
	 	LogOutput.traceLog("[doBettingRound] ends");
	}

	/**
	 * Returns the allowed actions of a specific player.
	 * 
	 * @param player
	 *            The player.
	 * 
	 * @return The allowed actions.
	 */
	public Set<Action> getAllowedActions(Player player) {
		Set<Action> actions = new HashSet<>();
		if (player.isAllIn()) {
			// actions.add(Action.CHECK);
		} else {
			BigDecimal actorBet = actor.getBet();
			BigDecimal minBetAmount = (minBet.subtract(actor.getBet())).add(minBetDiff);
			BigDecimal maxBetAmount = getMaxBetAmount(actor);

			// if(!actor.isBot())
			// System.out.println(actor.getName() + ": " + minBet.longValue() + "," +
			// actorBet.longValue() + "," + minBetAmount.longValue() + "," +
			// maxBetAmount.longValue());

			actions.add(Action.FOLD);
			if (actorBet.compareTo(minBet) < 0) {
				if (actor.getCash().compareTo(minBet.subtract(actor.getBet())) > 0)
					actions.add(Action.CALL);
				else {
					actions.add(Action.ALL_IN);
					return actions;
				}
			} else {
				actions.add(Action.CHECK);
			}

			if (maxBetAmount.compareTo(minBet.subtract(actor.getBet())) != 0) {
				if (maxBetAmount.compareTo(minBetAmount) > 0)
					actions.add(Action.RAISE);
				else
					actions.add(Action.ALL_IN);
			}
		}
		return actions;
	}

	public BigDecimal getMaxBetAmount(Player playerToNotify) {
		BigDecimal maxBetAmount = BigDecimal.ZERO;

		for (Player player : players) {
			if (player.playerStatus == PlayerStatus.ACTIVE && player.isActive) {
				if (player == playerToNotify)
					continue;
				if (maxBetAmount.compareTo(player.getCash().add(player.getBet())) < 0) {
					maxBetAmount = player.getCash().add(player.getBet());
				}
			}
		}

		if (maxBetAmount.compareTo(actor.getCash().add(actor.getBet())) > 0)
			maxBetAmount = actor.getCash().add(actor.getBet());
		maxBetAmount = maxBetAmount.subtract(actor.getBet());
		return maxBetAmount;
	}

	/**
	 * Contributes to the pot.
	 * 
	 * @param amount
	 *            The amount to contribute.
	 */
	private void contributePot(BigDecimal amount) {
		//for log trace
	 	LogOutput.traceLog("[contributePot] begins");
		for (Pot pot : pots) {
			if (!pot.hasContributer(actor)) {
				BigDecimal potBet = pot.getBet();
				if (amount.compareTo(potBet) >= 0) {
					// Regular call, bet or raise.
					pot.addContributer(actor);
					amount = amount.subtract(pot.getBet());
				} else {
					// Partial call (all-in); redistribute pots.
					pots.add(pot.split(actor, amount));
					amount = BigDecimal.ZERO;
				}
			}
			if (amount.compareTo(BigDecimal.ZERO) <= 0) {
				break;
			}
		}
		if (amount.compareTo(BigDecimal.ZERO) > 0) {
			Pot pot = new Pot(amount);
			pot.addContributer(actor);
			pots.add(pot);
		}
		//for log trace
	 	LogOutput.traceLog("[contributePot] ends");
	}

	private void contributePot(Player player, BigDecimal amount) {
		for (Pot pot : pots) {
			if (!pot.hasContributer(player)) {
				BigDecimal potBet = pot.getBet();
				if (amount.compareTo(potBet) >= 0) {
					// Regular call, bet or raise.
					pot.addContributer(player);
					amount = amount.subtract(pot.getBet());
				} else {
					// Partial call (all-in); redistribute pots.
					pots.add(pot.split(player, amount));
					amount = BigDecimal.ZERO;
				}
			}
			if (amount.compareTo(BigDecimal.ZERO) <= 0) {
				break;
			}
		}
		if (amount.compareTo(BigDecimal.ZERO) > 0) {
			Pot pot = new Pot(amount);
			pot.addContributer(player);
			pots.add(pot);
		}
	}

	/**
	 * Performs the showdown.
	 */
	private void doShowdown() {
		//for log trace
	 	LogOutput.traceLog("[doShowdown] begins");

		// Determine show order; start with all-in players...
		List<Player> showingPlayers = new ArrayList<>();
		for (Pot pot : pots) {
			for (Player contributor : pot.getContributors()) {
				if (!showingPlayers.contains(contributor) && contributor.isAllIn()) {
					showingPlayers.add(contributor);
				}
			}
		}
		// ...then last player to bet or raise (aggressor)...
		if (lastBettor != null) {
			if (!showingPlayers.contains(lastBettor)) {
				showingPlayers.add(lastBettor);
			}
		}
		// ...and finally the remaining players, starting left of the button.
		// int pos = (dealerPosition + 1) % activePlayerSize();
		int pos = getNextActivePos(dealerPosition);
		while (showingPlayers.size() < activePlayerSize()) {
			// Player player = activePlayers.get(pos);
			Player player = players[pos];
			if (!showingPlayers.contains(player)) {
				showingPlayers.add(player);
			}
			// pos = (pos + 1) % activePlayerSize();
			pos = getNextActivePos(pos);
		}

		// Players automatically show or fold in order.
		boolean firstToShow = true;
		int bestHandValue = -1;
		for (Player playerToShow : showingPlayers) {
			Hand hand = new Hand(board);
			hand.addCards(playerToShow.getCards());
			HandValue handValue = new HandValue(hand);
			
//			System.out.println(handValue.toString() + ": " + handValue.getValue());
			boolean doShow = ALWAYS_CALL_SHOWDOWN;
			if (!doShow) {
				if (playerToShow.isAllIn()) {
					// All-in players must always show.
					doShow = true;
					firstToShow = false;
				} else if (firstToShow) {
					// First player must always show.
					doShow = true;
					bestHandValue = handValue.getValue();
					firstToShow = false;
				} else {
					// Remaining players only show when having a chance to win.
					// if (handValue.getValue() >= bestHandValue) {
					doShow = true;
					bestHandValue = handValue.getValue();
					// }
				}
			}
			if (doShow) {
				// Show hand.
				notifyPlayersUpdated(true, false);
			} else {
				// Fold.
				playerToShow.setCards(null);
				playerToShow.isActive = false;
				// activePlayers.remove(playerToShow);
				notifyPlayersUpdated(false, false);
			}
		}
		delayTimer(0.5f);

		// Sort players by hand value (highest to lowest).
		Map<HandValue, List<Player>> rankedPlayers = new TreeMap<>();
		for (Player player : players) {
			if (player.playerStatus == PlayerStatus.ACTIVE && player.isActive) {
				// Create a hand with the community cards and the player's hole cards.
				Hand hand = new Hand(board);
				hand.addCards(player.getCards());
				// Store the player together with other players with the same hand value.
				HandValue handValue = new HandValue(hand);
				player.handValue = handValue.getDescription();
				player.setAction(null);
				// player.setAction(new Action(handValue.getDescription(),
				// handValue.getDescription()));
				List<Player> playerList = rankedPlayers.get(handValue);
				if (playerList == null) {
					playerList = new ArrayList<>();
				}
				playerList.add(player);
				rankedPlayers.put(handValue, playerList);
			}
		}

		// Per rank (single or multiple winners), calculate pot distribution.
		BigDecimal totalPot = getTotalPot();
		Map<Player, BigDecimal> potDivision = new HashMap<>();
		for (HandValue handValue : rankedPlayers.keySet()) {
			List<Player> winners = rankedPlayers.get(handValue);
			for (Pot pot : pots) {
				// Determine how many winners share this pot.
				int noOfWinnersInPot = 0;
				for (Player winner : winners) {
					if (pot.hasContributer(winner)) {
						noOfWinnersInPot++;
					}
				}
				if (noOfWinnersInPot > 0) {
					// Divide pot over winners.
					long divVal = pot.getValue().longValue() / noOfWinnersInPot;
					BigDecimal potShare = BigDecimal.valueOf(divVal);
					for (Player winner : winners) {
						if (pot.hasContributer(winner)) {
							BigDecimal oldShare = potDivision.get(winner);
							if (oldShare != null) {
								potDivision.put(winner, oldShare.add(potShare));
							} else {
								potDivision.put(winner, potShare);
							}

						}
					}
					// Determine if we have any odd chips left in the pot.
					BigDecimal oddChips = pot.getValue().remainder(new BigDecimal(String.valueOf(noOfWinnersInPot))); // TODO
					if (oddChips.compareTo(BigDecimal.ZERO) > 0) {
						// Divide odd chips over winners, starting left of the dealer.
						pos = dealerPosition;
						while (oddChips.compareTo(BigDecimal.ZERO) > 0) {
							// pos = (pos + 1) % activePlayerSize();
							pos = getNextActivePos(pos);
							// Player winner = activePlayers.get(pos);
							Player winner = players[pos];
							BigDecimal oldShare = potDivision.get(winner);
							if (oldShare != null) {
								potDivision.put(winner, oldShare.add(BigDecimal.ONE));
								oddChips = oddChips.subtract(BigDecimal.ONE);
							}
						}

					}
					pot.clear();
				}
			}
		}

		// Divide winnings.
		StringBuilder winnerText = new StringBuilder();
		float realPercentage = isProtable() ? 99.5f : 100;
		BigDecimal totalRealPot = totalPot.multiply(new BigDecimal(realPercentage)).divide(new BigDecimal(100));

		// distribute dealer share
		BigDecimal dealerShare = totalPot.subtract(totalRealPot);
		long dealerShareValue = dealerShare.longValue();
		if(isProtable()){
			gameExt.showDealerPot(dealerShareValue);
			delayTimer(2.5f);
			gameExt.dealerShareToDealer(dealerShareValue);
			delayTimer(1.0f);
		}

		
		for (Player winner : potDivision.keySet()) {
			gameExt.showWinnerCards(winner);
			delayTimer(2);
		}

		BigDecimal totalWon = BigDecimal.ZERO;
		for (Player winner : potDivision.keySet()) {
			BigDecimal potShare = potDivision.get(winner);
			BigDecimal potRealShare = potShare.multiply(new BigDecimal(realPercentage)).divide(new BigDecimal(100));
			potDivision.put(winner, potRealShare);
			totalWon = totalWon.add(potRealShare);
		}
		
		// if odded chips, distribute it
		BigDecimal oddChips = totalRealPot.subtract(totalWon);
		pos = dealerPosition;
		while (oddChips.compareTo(BigDecimal.ZERO) > 0) {
			pos = getNextActivePos(pos);
			Player winner = players[pos];
			BigDecimal oldShare = potDivision.get(winner);
			if (oldShare != null) {
				potDivision.put(winner, oldShare.add(BigDecimal.ONE));
				oddChips = oddChips.subtract(BigDecimal.ONE);
			}
		}
		
		totalWon = BigDecimal.ZERO;
		for (Player winner : potDivision.keySet()) {
			BigDecimal potShare = potDivision.get(winner);
			winner.win(potShare);
			totalWon = totalWon.add(potShare);
			if (winnerText.length() > 0) {
				winnerText.append(", ");
			}
			winnerText.append(String.format("%s wins $ %d", winner, potShare.intValue()));

			Hand hand = new Hand(board);
			hand.addCards(winner.getCards());
			HandValue handValue = new HandValue(hand);

			BigDecimal remainPot = totalRealPot.subtract(totalWon);

			gameExt.updateHandHistory(winner.getName(), potShare.longValue(), handValue);
			gameExt.payWinnerChips(winner, potShare.longValue(), handValue, remainPot.longValue());

			showPlayer = winner;
			winner.isShow = true;
			winner.isWinner = true;
			gameExt.showBestCards();
			winner.isWinner = false;
			showPlayer = null;

			delayTimer(4);
		}

		gameExt.hideWinners();
		delayTimer(0.5f);

		winnerText.append('.');
		//for log trace
	 	LogOutput.traceLog("[doShowdown] ends");
	}

	private void updatePlayInfo() {
		//for log trace
	 	LogOutput.traceLog("[updatePlayInfo] begins");
		for (Player player : players) {
			if (player.isBot())
				continue;
			if (player.playerStatus == PlayerStatus.ACTIVE) {
				gameExt.addPlayInfo(player.getEmail(), player.winCash.longValue());
				gameExt.update_daily_hand(player.getEmail(), player.winCash.longValue());
			}
		}
		//for log trace
	 	LogOutput.traceLog("[updatePlayInfo] ends");
	}

	/**
	 * Notifies listeners with a custom game message.
	 * 
	 * @param message
	 *            The formatted message.
	 * @param args
	 *            Any arguments.
	 */

	private void notifyMessage(Player playerToNotify, String message) {
		playerToNotify.getClient().messageReceived(playerToNotify, message);
	}

	/**
	 * Notifies clients that the board has been updated.
	 */
	private void notifyBoardUpdated() {
		//for log trace
	 	LogOutput.traceLog("[notifyBoardUpdated] begins");
		gameExt.updateBoard(0, false);
		gameExt.showBestCards();
		//for log trace
	 	LogOutput.traceLog("[notifyBoardUpdated] ends");
	}

	/**
	 * Returns the total pot size.
	 * 
	 * @return The total pot size.
	 */
	public BigDecimal getTotalPot() {
		BigDecimal totalPot = BigDecimal.ZERO;
		for (Pot pot : pots) {
			totalPot = totalPot.add(pot.getValue());
		}
		return totalPot;
	}

	/**
	 * Notifies clients that one or more players have been updated. <br />
	 * <br />
	 * 
	 * A player's secret information is only sent its own client; other clients see
	 * only a player's public information.
	 * 
	 * @param showdown
	 *            Whether we are at the showdown phase.
	 */
	private void notifyPlayersUpdated(boolean showdown, boolean isDeal) {
		//for log trace
	 	LogOutput.traceLog("[notifyPlayersUpdated] begins");
		for (Player playerToNotify : players) {
			for (Player player : players) {
				if (player.playerStatus == PlayerStatus.NONE)
					continue;
				if (playerToNotify.isBot()) {
					if (!showdown && !player.equals(playerToNotify)) {
						// Hide secret information to other players.
						player = player.publicClone();
					}
					playerToNotify.getClient().playerUpdated(playerToNotify, player);
				}
			}
		}
		gameExt.updatePlayers(showdown, isDeal);
		gameExt.showBestCards();
		//for log trace
	 	LogOutput.traceLog("[notifyPlayersUpdated] ends");
	}

	private void delayTimer(float _t) {
		//for log trace
	 	LogOutput.traceLog("[table->delayTimer] begins");
		delayFlag = true;
		// SetTimer
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// Your database code here
				if (delayFlag) {
					delayFlag = false;
					synchronized (monitor) {
						monitor.notifyAll();
					}
				}
			}
		}, (int) (_t * 1000));

		while (delayFlag) {
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
		//for log trace
	 	LogOutput.traceLog("[table->delayTimer] ends");
	}

}
