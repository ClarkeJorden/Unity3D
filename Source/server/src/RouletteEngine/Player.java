package RouletteEngine;

import java.util.ArrayList;
import java.util.List;

public class Player implements Comparable<Player> {

	private int pos;
	private String email;
	private String name;
	private long cash;
	public long bet;
	public int giftCategory;
	public int giftDetail;
	private List<Bet> betList;
	public List<Bet> prevBetList;

	public boolean isReady = false;
	public boolean isPlayed = false;

	public Player(int pos, String email, String name, long cash, int _giftCategory, int _giftValue) {
		this.pos = pos;
		this.email = email;
		this.name = name;
		this.cash = cash;
		this.bet = 0;
		giftCategory = _giftCategory;
		giftDetail = _giftValue;
		betList = new ArrayList<>();
		prevBetList = new ArrayList<>();
	}

	@Override
	public int compareTo(Player compPlayer) {
		/* For Ascending order*/
		return Integer.compare(this.pos, compPlayer.pos);
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

	public long getCash() {
		return cash;
	}

	public void setCash(long d) {
		cash = d;
	}

	public void addCash(long amount) {
		cash += amount;
	}

	public void payCash(long amount) {
		if (amount > cash) {
			throw new IllegalStateException("Player asked to pay more cash than he owns!");
		}
		cash -= amount;
	}

	public long getBet() {
		return bet;
	}

	public void setBet(long d) {
		bet = d;
	}

	public void addBet(long d) {
		bet += d;
	}

	public void payBet(long d) {
		bet -= d;
	}

	public List<Bet> getBetList() {
		return betList;
	}

}
