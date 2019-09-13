package RouletteEngine;

public enum BetType {

	STRAIGHT_UP(0, 35),
	SPLIT_VER(1, 17),
	SPLIT_HOR(2, 17),
	CORNER(3, 8),
	STREET(4, 11),
	SIX_LINE(5, 5),
	COLUMN(6, 2),
	DOZEN(7, 2),
	EVEN_CHANCE(8, 1),
	ZERO(9, 35),
	ZERO_TWO(10, 17),
	ZERO_THREE(11, 11),
	ZERO_FOUR(12, 8),
	ZERO_FIVE(13, 6),

	;

	private int id;
	private int payout;

	BetType(int id, int p) {
		this.id = id;
		this.payout = p;
	}

	public int getId() {
		return id;
	}

	public int getPayout() {
		return payout;
	}

}
