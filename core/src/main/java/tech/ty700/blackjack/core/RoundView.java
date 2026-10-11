package tech.ty700.blackjack.core;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

/**
 * Read-only snapshot of a round, safe to send to any client.
 * The dealer's hole card is left out until the dealer's turn.
 *
 * dealerTotal and activeSeat are null when hidden or when nobody is acting.
 */
public record RoundView(
        Round.Phase phase,
        List<Card> dealerCards,
        int dealerHiddenCards,
        Integer dealerTotal,
        List<SeatView> seats,
        Integer activeSeat)
{
    /* bet is null when the player sat out. activeHand is null unless it's this seat's turn */
    public record SeatView(
            int seat,
            UUID playerId,
            String playerName,
            BigInteger bet,
            boolean left,
            List<HandView> hands,
            Integer activeHand) {}

    public record HandView(
            List<Card> cards,
            int total,
            boolean soft,
            BigInteger bet,
            Hand.HandState state,
            Hand.HandResult result) {}
}
