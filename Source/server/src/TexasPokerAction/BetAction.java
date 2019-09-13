package TexasPokerAction;

import java.math.BigDecimal;

public class BetAction extends Action {

    /**
     * Constructor.
     * 
     * @param amount
     *            The amount to bet.
     */
    public BetAction(BigDecimal amount) {
        super("Bet", "bets", amount);
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Bet(%d)", getAmount());
    }
    
}
