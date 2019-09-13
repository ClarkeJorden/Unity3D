package TexasPokerBot;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import TexasPokerAction.Action;
import TexasPokerAction.BetAction;
import TexasPokerAction.RaiseAction;
import TexasPokerEngine.Card;
import TexasPokerEngine.Hand;
import TexasPokerEngine.HandValue;
import TexasPokerEngine.HandValueType;
import TexasPokerEngine.Player;
import TexasPokerEngine.TableType;
import TexasPokerUtil.PokerUtils;

public class BasicBot extends Bot {

	/** Tightness (0 = loose, 100 = tight). */
	private final int tightness;

	/** Betting aggression (0 = safe, 100 = aggressive). */
	private final int aggression;

	/** Table type. */
	private TableType tableType;

	/** The hole cards. */
	private Card[] cards;

	private static int BOT_WAITING_TIME = 2;
	private Timer timer;
	private boolean delayFlag;
	private final Object monitor = new Object();

	/**
	 * Constructor.
	 * 
	 * @param tightness
	 *            The bot's tightness (0 = loose, 100 = tight).
	 * @param aggression
	 *            The bot's aggressiveness in betting (0 = careful, 100 =
	 *            aggressive).
	 */
	public BasicBot(int tightness, int aggression) {
		if (tightness < 0 || tightness > 100) {
			throw new IllegalArgumentException("Invalid tightness setting");
		}
		if (aggression < 0 || aggression > 100) {
			throw new IllegalArgumentException("Invalid aggression setting");
		}
		this.tightness = tightness;
		this.aggression = aggression;
	}

	/** {@inheritDoc} */
	@Override
	public void joinedTable(Player playerToNotify, TableType type, BigDecimal bigBlind, List<Player> players) {
		this.tableType = type;
	}

	/** {@inheritDoc} */
	@Override
	public void messageReceived(Player playerToNotify, String message) {
		// Not implemented.
	}

	/** {@inheritDoc} */
	@Override
	public void handStarted(Player playerToNotify, Player dealer) {
		cards = null;
	}

	@Override
	public void setBlind(Player playerToNotify, Player blind, String blindText)
	{
	}

	/** {@inheritDoc} */
	@Override
	public void actorRotated(Player playerToNotify, Player actor) {
		// Not implemented.
	}

	@Override
	public void selectActor(Player playerToNofity, Player actor, boolean show)
	{
	}

	/** {@inheritDoc} */
	@Override
	public void boardUpdated(Player playerToNotify, List<Card> cards, BigDecimal bet, BigDecimal pot) {
		// Not implemented.
	}

	/** {@inheritDoc} */
	@Override
	public void playerUpdated(Player playerToNotify, Player player) {
		if (player.getCards().length == NO_OF_HOLE_CARDS) {
			this.cards = player.getCards();
		}
	}

	/** {@inheritDoc} */
	@Override
	public void playerActed(Player playerToNotify, Player player) {
		// Not implemented.
	}

	/** {@inheritDoc} */
	@Override
	public Action act(Player playerToNotify, BigDecimal bigBlind, BigDecimal currentBet, BigDecimal betDiff, BigDecimal maxBetAmount, List<Card> board, Set<Action> allowedActions) {
		Action action = Action.CHECK;

		Random rd = new Random();
		int t = rd.nextInt(17) + tightness;
		int rdVal = rd.nextInt(1);
		BigDecimal minBet = (currentBet.subtract(playerToNotify.getBet())).add(betDiff);
		long amount = (long)(minBet.longValue() * (1 + (float)(aggression + rd.nextInt(30)) / 40));
		amount = (long)Math.round((double)amount / (bigBlind.longValue() / 2)) * (bigBlind.longValue() / 2);
		if(amount > maxBetAmount.longValue())
			amount = maxBetAmount.longValue();

		Hand hand = new Hand(board);
		HandValue boardValue = new HandValue(hand);
		hand.addCards(cards);
		HandValue handValue = new HandValue(hand);

		if(board.size() == 0)			// PreFlop
		{
			if(handValue.getType() == HandValueType.ONE_PAIR) {
				if(allowedActions.contains(Action.ALL_IN)) {
					if(playerToNotify.getCash().compareTo(currentBet.add(currentBet)) > 0 && t >= 90)
						action = Action.FOLD;
					else
						action = Action.ALL_IN;
				}
				else if(allowedActions.contains(Action.CALL))
					action = Action.CALL;
				else if(allowedActions.contains(Action.RAISE) && currentBet.compareTo(playerToNotify.getBet()) == 0) {
					action = new RaiseAction(BigDecimal.valueOf(amount));
				}
				else if(allowedActions.contains(Action.CHECK)) {
					action = Action.CHECK;
				}
				else {
					action = Action.FOLD;
				}
			}
			else {
				if(allowedActions.contains(Action.ALL_IN)) {
					if(playerToNotify.getCash().compareTo(currentBet.add(currentBet)) > 0 && t >= 80)
						action = Action.FOLD;
					else
						action = Action.ALL_IN;
				}
				else if(cards[0].getRank() + cards[1].getRank() < 8 && Math.abs(cards[0].getRank() - cards[1].getRank()) > 2) {
					action = Action.FOLD;
				}
				else {
					if(allowedActions.contains(Action.CALL)) {
						action = Action.CALL;
					}
					else if(allowedActions.contains(Action.CHECK)) {
						action = Action.CHECK;
					}
					else {
						action = Action.FOLD;
					}
				}
			}
		}
		else
		{
			if(board.size() == 3)			// Flop
			{
				if(allowedActions.contains(Action.ALL_IN))
				{
					if(playerToNotify.getCash().compareTo(currentBet.add(currentBet)) < 0)
						action = Action.ALL_IN;
					else
					{
						if(handValue.getType().getValue() >= 1)
							action = Action.ALL_IN;
						else
							action = Action.FOLD;
					}
				}
				else if( allowedActions.contains(Action.RAISE) && 
						handValue.getType().getValue() - boardValue.getType().getValue() > 0 &&
						currentBet.compareTo(playerToNotify.getBet()) == 0 &&
					( (0 <= t && t < 60 && handValue.getType().getValue() >= 1)
					|| (60 <= t && handValue.getType().getValue() >= 2) ) )
				{
					action = new RaiseAction(BigDecimal.valueOf(amount));
				}
				else if( allowedActions.contains(Action.CALL) &&
						( (0 <= t && t < 60)
						|| (60 <= t && handValue.getType().getValue() >= 1) ) )
				{
					action = Action.CALL;
				}
				else if( allowedActions.contains(Action.CHECK) &&
						( (0 <= t && t < 90)
						|| (90 <= t && handValue.getType().getValue() >= 1) ) )
				{
					action = Action.CHECK;
				}
				else
					action = Action.FOLD;
			}
			else if(board.size() == 4)		// Turn
			{
				if(allowedActions.contains(Action.ALL_IN))
				{
					if(playerToNotify.getCash().compareTo(currentBet.add(currentBet)) < 0)
						action = Action.ALL_IN;
					else
					{
						if(0 <= t && t < 40 && handValue.getType().getValue() >= 1)
							action = Action.ALL_IN;
						else if(40 <= t && t < 80 && handValue.getType().getValue() >= 2)
							action = Action.ALL_IN;
						else if(80 <= t && handValue.getType().getValue() >= 3)
							action = Action.ALL_IN;
						else
							action = Action.FOLD;
					}
				}
				else if( allowedActions.contains(Action.RAISE) &&
						handValue.getType().getValue() - boardValue.getType().getValue() > 0 &&
						currentBet.compareTo(playerToNotify.getBet()) == 0 &&
					( (0 <= t && t < 60 && handValue.getType().getValue() >= 2)
					|| (60 <= t && handValue.getType().getValue() >= 3) ) )
				{
					action = new RaiseAction(BigDecimal.valueOf(amount));
				}
				else if( allowedActions.contains(Action.CALL) &&
						( (0 <= t && t < 60 && handValue.getType().getValue() >= 1)
						|| (60 <= t && handValue.getType().getValue() >= 2) ) )
				{
					action = Action.CALL;
				}
				else if( allowedActions.contains(Action.CHECK) &&
						( (0 <= t && t < 60)
						|| (60 <= t && handValue.getType().getValue() >= 1) ) )
				{
					action = Action.CHECK;
				}
				else
					action = Action.FOLD;
			}
			else if(board.size() == 5)		// River
			{
				if(allowedActions.contains(Action.ALL_IN))
				{
					if(playerToNotify.getCash().compareTo(currentBet.add(currentBet)) < 0)
						action = Action.ALL_IN;
					else
					{
						if(0 <= t && t < 40 && handValue.getType().getValue() >= 2)
							action = Action.ALL_IN;
						else if(40 <= t && t < 80 && handValue.getType().getValue() >= 3)
							action = Action.ALL_IN;
						else if(80 <= t && handValue.getType().getValue() >= 4)
							action = Action.ALL_IN;
						else
							action = Action.FOLD;
					}
				}
				else if( allowedActions.contains(Action.RAISE) &&
						handValue.getType().getValue() - boardValue.getType().getValue() > 0 &&
						currentBet.compareTo(playerToNotify.getBet()) == 0 &&
					( (0 <= t && t < 60 && handValue.getType().getValue() >= 3)
					|| (60 <= t && handValue.getType().getValue() >= 4) ) )
				{
					action = new RaiseAction(BigDecimal.valueOf(amount));
				}
				else if( allowedActions.contains(Action.CALL) &&
						( (0 <= t && t < 60 && handValue.getType().getValue() >= 2)
						|| (60 <= t && handValue.getType().getValue() >= 3) ) )
				{
					action = Action.CALL;
				}
				else if( allowedActions.contains(Action.CHECK) &&
						( (0 <= t && t < 60 && handValue.getType().getValue() >= 1)
						|| (60 <= t && handValue.getType().getValue() >= 2) ) )
				{
					action = Action.CHECK;
				}
				else
					action = Action.FOLD;
			}
		}

		delayTimer();
		return action;
	}

	private void delayTimer() {
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
		}, BOT_WAITING_TIME * 1000);

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

}
