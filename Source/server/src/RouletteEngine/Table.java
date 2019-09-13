package RouletteEngine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import RouletteExtension.RoomExtension;

public class Table {

	private final RoomExtension gameExt;

	public List<Player> players;
	public List<Player> newPlayers;
	public List<Integer> spinList;

	public int wheelValue;
	public boolean isRunning = false;
	public Status status = Status.WAITING;

	public Timer timer;
	public boolean delayFlag;
	public final Object monitor = new Object();

	public Table(RoomExtension roomExt) {
		this.gameExt = roomExt;
		players = new ArrayList<>();
		newPlayers = new ArrayList<>();
		spinList = new ArrayList<>();
		status = Status.BET;
	}

	public int playerSize() {
		return players.size() + newPlayers.size();
	}

	private void delayTimer(float _t) {
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
		}, (long) (_t * 1000));

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
	}

	public void stopDelay() {
		if (delayFlag) {
			delayFlag = false;
			synchronized (monitor) {
				monitor.notifyAll();
			}
		}
	}

	public void run() {
		isRunning = true;
		while (true) {
			for (Player player : newPlayers) {
				players.add(player);
			}
			newPlayers.clear();
			Collections.sort(players);
			gameExt.updatePlayers();
			if (players.size() == 0)
				break;
			playHand();
		}
		isRunning = false;
		status = Status.WAITING;
		System.out.println("waiting");
	}

	public void playHand() {
		initHand();
		doBetting();
		doSpin();
		doPayout();
	}

	public void initHand() {
		reset();
		for (Player player : players) {
			player.isPlayed = false;
		}
	}

	public void reset() {
		for (Player player : players) {
			player.isReady = false;
		}
	}

	public void doBetting() {
		status = Status.BET;
		gameExt.updateStatus(true);
		gameExt.sendServerMessage("", "Dealer: Place your bets, please.", true, 0);
		delayTimer(32f);

		boolean isAllReady = true;
		for (Player p : players) {
			if (!p.isReady)
				isAllReady = false;
		}
		if (!isAllReady) {
			gameExt.sendServerMessage("", "Dealer: Last bets!", true, 1);
			delayTimer(8f);
		}

		gameExt.sendServerMessage("", "Dealer: No more bets!", true, 2);

		for (Player player : players)
			player.isReady = true;

		gameExt.updatePlayers();

		reset();
	}

	public void doSpin() {
		status = Status.SPIN;
		Random rand = new Random();
		wheelValue = rand.nextInt(37);
		System.out.println("spin : " + wheelValue);
		gameExt.updateStatus(true);
		delayTimer(12f);
		reset();

		if (spinList.size() == 15)
			spinList.remove(0);
		spinList.add(Integer.valueOf(wheelValue));
		gameExt.updateStoryboard();

		int count = 0;
		for (Player player : players) {
			List<Bet> betList = player.getBetList();
			boolean isWin = false;
			count += betList.size();
			player.isPlayed = (betList.size() > 0);
			for (Bet bet : betList) {
				if (betList == null)
					System.out.println("Spin betList null: " + player.getEmail() + "," + bet.type + "," + bet.value);
				if (bet == null)
					System.out.println("Spin bet null: " + player.getEmail());
				if (bet.isMatch(wheelValue)) {
					isWin = true;
					break;
				}
			}
			if (isWin)
				gameExt.sendServerMessage(player.getEmail(), "Dealer: You win!", true, 3);
			else if (count > 0)
				gameExt.sendServerMessage(player.getEmail(), "Dealer: No bets win!", true, 4);
		}
	}

	public void doPayout() {
		status = Status.PAYOUT;
		gameExt.updateStatus(true);
		gameExt.updatePlayers();

		delayTimer(6f);

		// Remove bets
		for (Player player : players) {
			List<Bet> betList = player.getBetList();
			List<Bet> prevBetList = player.prevBetList;
			List<Bet> tempList = new ArrayList<>();
			if (betList.size() > 0)
				prevBetList.clear();
			for (Bet bet : betList) {
				if (betList == null)
					System.out.println("Payout betList null: " + player.getEmail() + "," + bet.type + "," + bet.value);
				if (bet == null)
					System.out.println("Payout bet null: " + player.getEmail());
				prevBetList.add(bet);
				if (bet.isMatch(wheelValue)) {
					// System.out.println("match : " + bet.type + "," + bet.value + "," +
					// bet.amount);
					tempList.add(bet);
				}
			}
			player.setBet(0);
			betList.clear();
			for (Bet bet : tempList) {
				betList.add(bet);
				player.addBet(bet.amount);
			}
		}
		gameExt.removeBets();
		// gameExt.updateBets();
		delayTimer(3f);

		// Payout
		List<String> emailList = new ArrayList<>();
		for (Player player : players)
			emailList.add(player.getEmail());
		for (String email : emailList) {
			if (players.size() == 0)
				continue;
			for (Player player : players) {
				if (player.getEmail() == email) {
					List<Bet> betList = player.getBetList();
					long sum = 0, count = 0;
					for (Bet bet : betList) {
						if (betList == null)
							System.out.println(
									"Payout1 betList null: " + player.getEmail() + "," + bet.type + "," + bet.value);
						if (bet == null)
							System.out.println("Payout1 bet null: " + player.getEmail());
						sum += bet.getAmount() * bet.getType().getPayout();
						count += bet.getType().getPayout();
					}
					player.addCash(sum);
					if (sum > 0) {
						gameExt.doPayout(player.getPos(), sum + player.getBet());
						delayTimer(3f + count * 0.02f + (count / 15) * 0.6f);
						// gameExt.updatePlayers();
						// delayTimer(1.5f);
					}
					if (player.isPlayed)
						gameExt.addPlayInfo(email, sum + player.getBet());
				}
			}
		}
		delayTimer(1f);
	}

}
