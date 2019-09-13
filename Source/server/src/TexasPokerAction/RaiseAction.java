package TexasPokerAction;

import java.math.BigDecimal;

public class RaiseAction extends Action {

    /**
     * Constructor.
     * 
     * @param amount
     *            The amount to raise with.
     */
    public RaiseAction(BigDecimal amount) {
        super("Raise", "raises", amount);
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Raise(%d)", getAmount());
    }
    
}
