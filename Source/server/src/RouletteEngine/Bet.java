package RouletteEngine;

public class Bet {

	public BetType type;
	public long amount;
	public int value;

	public static int[] colors = {
			1, 
			0, 1, 0,
			1, 0, 1,
			0, 1, 0,
			1, 1, 0,
			1, 0, 1,
			0, 1, 0,
			0, 1, 0,
			1, 0, 1,
			0, 1, 0,
			1, 1, 0,
			1, 0, 1,
			0, 1, 0,
	}; 

	public Bet(int t, long a, int v) {

		if(t == 0)			type = BetType.STRAIGHT_UP;
		else if(t == 1)		type = BetType.SPLIT_VER;
		else if(t == 2)		type = BetType.SPLIT_HOR;
		else if(t == 3)		type = BetType.CORNER;
		else if(t == 4)		type = BetType.STREET;
		else if(t == 5)		type = BetType.SIX_LINE;
		else if(t == 6)		type = BetType.COLUMN;
		else if(t == 7)		type = BetType.DOZEN;
		else if(t == 8)		type = BetType.EVEN_CHANCE;
		else if(t == 9)		type = BetType.ZERO;
		else if(t == 10)	type = BetType.ZERO_TWO;
		else if(t == 11)	type = BetType.ZERO_THREE;
		else if(t == 12)	type = BetType.ZERO_FOUR;
		else if(t == 13)	type = BetType.ZERO_FIVE;

		this.amount = a;
		this.value = v;
	}

	public BetType getType() {
		return type;
	}

	public long getAmount() {
		return amount;
	}

	public boolean isMatch(int wheel)
	{
		if(type == BetType.STRAIGHT_UP) {
			return (value == wheel);
		} else if(type == BetType.SPLIT_VER) {
			if(value == wheel || value + 1 == wheel)
				return true;
		} else if(type == BetType.SPLIT_HOR) {
			if(value == wheel || value - 3 == wheel)
				return true;
		} else if(type == BetType.CORNER) {
			if(value == wheel || value - 3 == wheel || value - 2 == wheel || value + 1 == wheel)
				return true;
		} else if(type == BetType.STREET) {
			int min = value * 3 - 2;
			int max = value * 3;
			if(wheel >= min && wheel <= max)
				return true;
		} else if(type == BetType.SIX_LINE) {
			int min = value * 3 - 2;
			int max = value * 3 + 3;
			if(wheel >= min && wheel <= max)
				return true;
		} else if(type == BetType.COLUMN) {
			if(wheel % 3 == value % 3)
				return true;
		} else if(type == BetType.DOZEN) {
			int min = value * 12 - 11;
			int max = value * 12;
			if(wheel >= min && wheel <= max)
				return true;
		} else if(type == BetType.EVEN_CHANCE) {
			if(wheel >= 1 && wheel <= 36) {
				if(value == 0 || value == 1) {			// red or blue
					return (colors[wheel] == value);
				} else if(value == 2 || value == 3) {	// even or odd
					return (wheel % 2 == value - 2);
				} else if(value == 4 || value == 5) {	// low or high
					return ((wheel - 1) / 18 == value - 4);
				}
			}
		} else if(type == BetType.ZERO) {
			return (wheel == 0);
		} else if(type == BetType.ZERO_TWO) {
			return (wheel == 0 || wheel == value);
		} else if(type == BetType.ZERO_THREE) {
			return (wheel == 0 || wheel == value || wheel == value + 1);
		} else if(type == BetType.ZERO_FOUR || type == BetType.ZERO_FIVE) {
			return (wheel <= 3);
		}
		return false;
	}

}
