package tech.ty700.blackjack.core;

import java.util.List;
import java.util.Set;

/**
 * Standard multi-deck basic strategy (dealer hits soft 17), for bots.
 * Pure logic: give it a hand, the dealer's up card, and what's legal, and it picks a move.
 */
public final class BasicStrategy
{
    private BasicStrategy() {}

    /* The chart sometimes says "double if allowed, otherwise ..." */
    private enum Move { HIT, STAND, SPLIT, DOUBLE_OR_HIT, DOUBLE_OR_STAND }

    public static Action choose(final RoundView.HandView hand, final Card dealerUp, final Set<Action> legal)
    {
        return choose(hand.cards(), hand.total(), hand.soft(), dealerUp, legal);
    }

    public static Action choose(final List<Card> cards, final int total, final boolean soft,
                                final Card dealerUp, final Set<Action> legal)
    {
        /* Ace counts 11 here, so up is 2 to 11 */
        int up = dealerUp.getRankValue();

        Move move = null;
        boolean pair = cards.size() == 2 && cards.get(0).getRankValue() == cards.get(1).getRankValue();
        if (legal.contains(Action.SPLIT) && pair)
        {
            move = pair(cards.getFirst().getRankValue(), up);
        }
        if (move == null)
        {
            move = soft ? softTotal(total, up) : hardTotal(total, up);
        }

        return switch (move)
        {
            case SPLIT -> Action.SPLIT;
            case STAND -> Action.STAND;
            case HIT -> legal.contains(Action.HIT) ? Action.HIT : Action.STAND;
            case DOUBLE_OR_HIT -> legal.contains(Action.DOUBLE) ? Action.DOUBLE
                    : legal.contains(Action.HIT) ? Action.HIT : Action.STAND;
            case DOUBLE_OR_STAND -> legal.contains(Action.DOUBLE) ? Action.DOUBLE : Action.STAND;
        };
    }

    /* null means "don't split, play it as a total" */
    private static Move pair(final int value, final int up)
    {
        return switch (value)
        {
            case 11, 8 -> Move.SPLIT;
            case 9 -> (up <= 6 || up == 8 || up == 9) ? Move.SPLIT : null;
            case 7, 3, 2 -> up <= 7 ? Move.SPLIT : null;
            case 6 -> up <= 6 ? Move.SPLIT : null;
            case 4 -> (up == 5 || up == 6) ? Move.SPLIT : null;
            /* 5s play as hard 10, 10s as hard 20 */
            default -> null;
        };
    }

    private static Move softTotal(final int total, final int up)
    {
        if (total >= 20) return Move.STAND;
        return switch (total)
        {
            case 19 -> up == 6 ? Move.DOUBLE_OR_STAND : Move.STAND;
            case 18 -> up <= 6 ? Move.DOUBLE_OR_STAND : (up <= 8 ? Move.STAND : Move.HIT);
            case 17 -> (up >= 3 && up <= 6) ? Move.DOUBLE_OR_HIT : Move.HIT;
            case 16, 15 -> (up >= 4 && up <= 6) ? Move.DOUBLE_OR_HIT : Move.HIT;
            case 14, 13 -> (up == 5 || up == 6) ? Move.DOUBLE_OR_HIT : Move.HIT;
            default -> Move.HIT;
        };
    }

    private static Move hardTotal(final int total, final int up)
    {
        if (total >= 17) return Move.STAND;
        if (total >= 13) return up <= 6 ? Move.STAND : Move.HIT;
        return switch (total)
        {
            case 12 -> (up >= 4 && up <= 6) ? Move.STAND : Move.HIT;
            case 11 -> Move.DOUBLE_OR_HIT;
            case 10 -> up <= 9 ? Move.DOUBLE_OR_HIT : Move.HIT;
            case 9 -> (up >= 3 && up <= 6) ? Move.DOUBLE_OR_HIT : Move.HIT;
            default -> Move.HIT;
        };
    }
}
