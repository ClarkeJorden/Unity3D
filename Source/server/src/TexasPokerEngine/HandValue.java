package TexasPokerEngine;

import java.util.ArrayList;
import java.util.List;

import TexasPokerEngine.LogOutput;

public class HandValue implements Comparable<HandValue> {

	/**
	 * The hand.
	 */
	private final Hand hand;

	/**
	 * The hand value type.
	 */
	private final HandValueType type;

	/**
	 * The exact, numeric hand value.
	 */
	private final int value;

	private List<Card> cards = new ArrayList<>();
	public List<Card> wholeCards = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param hand The hand.
	 */
	public HandValue(Hand hand) {
		//for log trace
	 	LogOutput.traceLog("[HandValue] begins");
		this.hand = hand;
		HandEvaluator evaluator = new HandEvaluator(hand);
		type = evaluator.getType();
		value = evaluator.getValue();
		cards = evaluator.getBestCards();
		wholeCards = evaluator.getBestWholeCards();
		//for log trace
	 	LogOutput.traceLog("[HandValue] ends");
	}

	/**
	 * Returns the hand.
	 *
	 * @return The hand.
	 */
	public Hand getHand() {
		return hand;
	}

	public List<Card> getBestCards()
	{
		return cards;
	}

	/**
	 * Returns the hand value type.
	 *
	 * @return The hand value type.
	 */
	public HandValueType getType() {
		return type;
	}

	/**
	 * Returns a description of the hand value type.
	 *
	 * @return The description of the hand value type.
	 */
	public String getDescription() {
		return type.getDescription();
	}

	/**
	 * Returns the exact, numeric hand value.
	 *
	 * @return The exact, numeric hand value.
	 */
	public int getValue() {
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof HandValue && ((HandValue) obj).getValue() == value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(HandValue handValue) {
		if (value > handValue.getValue()) {
			return -1;
		} else if (value < handValue.getValue()) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		String str = type.getDescription() + ":";
		for(Card card : wholeCards)
			str += " " + card.toDescriptionString();
		return str;
	}
	
	public String getWholeCardString()
	{
		String str = "";
		for(Card card : wholeCards)
		{
			if(str.compareTo("") == 0)
				str += card.toDescriptionString();
			else
				str += " " + card.toDescriptionString();
		}
		return str;
	}
}
