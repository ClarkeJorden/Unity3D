package TexasPokerEngine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import TexasPokerAction.Action;

public interface Client {
    
    /**
     * Handles a game message.
     * 
     * @param message
     *            The message.
     */
    void messageReceived(Player playerToNotify, String message);

    /**
     * Handles the player joining a table.
     * 
     * @param type
     *            The table type (betting structure).
     * @param bigBlind
     *            The table's big blind.
     * @param players
     *            The players at the table (including this player).
     */
    void joinedTable(Player playerToNotify, TableType type, BigDecimal bigBlind, List<Player> players);
    
    /**
     * Handles the start of a new hand.
     * 
     * @param dealer
     *            The dealer.
     */
    void handStarted(Player playerToNotify, Player dealer);
    
    void setBlind(Player playerToNotify, Player blind, String blindText);
    
    /**
     * Handles the rotation of the actor (the player who's turn it is).
     * 
     * @param actor
     *            The new actor.
     */
    void actorRotated(Player playerToNotify, Player actor);

    void selectActor(Player playerToNofity, Player actor, boolean show);
    
    /**
     * Handles an update of this player.
     * 
     * @param player
     *            The player.
     */
    void playerUpdated(Player playerToNotify, Player player);
    
    /**
     * Handles an update of the board.
     * 
     * @param cards
     *            The community cards.
     * @param bet
     *            The current bet.
     * @param pot
     *            The current pot.
     */
    void boardUpdated(Player playerToNotify, List<Card> cards, BigDecimal bet, BigDecimal pot);
    
    /**
     * Handles the event of a player acting.
     * 
     * @param player
     *            The player that has acted.
     */
    void playerActed(Player playerToNotify, Player player);

    /**
     * Requests this player to act, selecting one of the allowed actions.
     * 
     * @param minBet
     *            The minimum bet.
     * @param currentBet
     *            The current bet.
     * @param allowedActions
     *            The allowed actions.
     * 
     * @return The selected action.
     */
    Action act(Player playerToNotify, BigDecimal arg0, BigDecimal arg1, BigDecimal arg2, BigDecimal arg3, List<Card> board, Set<Action> allowedActions);

}
