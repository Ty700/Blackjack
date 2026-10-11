package tech.ty700.blackjack.cli;

import tech.ty700.blackjack.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * core deals instantly, so the CLI fakes the pace: it compares what's on screen with the new view
 * and builds one frame per card, in the order a real dealer would deal them.
 *
 * Frames hide totals until a hand has all its cards, and hide results until the last frame.
 */
final class DealAnimation
{
    private DealAnimation() {}

    /* One card appearing (or the hole card flipping) */
    record Frame(RoundView view, boolean flip) {}

    /* Cards shown so far, per seat and hand, plus the dealer */
    private static final class Shown
    {
        final List<List<Integer>> hands = new ArrayList<>();
        int dealerVisible;
        int dealerHidden;
    }

    /* onScreen is null at the start of a round */
    static List<Frame> frames(final RoundView onScreen, final RoundView next)
    {
        List<Frame> frames = new ArrayList<>();
        Shown shown = startingPoint(onScreen, next);

        int dealerTotal = next.dealerCards().size() + next.dealerHiddenCards();
        boolean dealtBefore = onScreen != null && onScreen.dealerCards().size() + onScreen.dealerHiddenCards() > 0;

        /* Opening deal: everyone's first card, dealer up card, everyone's second card, dealer hole card */
        if (!dealtBefore && dealerTotal > 0)
        {
            for (int pass = 0; pass < 2; pass++)
            {
                for (int s = 0; s < next.seats().size(); s++)
                {
                    if (next.seats().get(s).hands().isEmpty()) continue;
                    if (shown.hands.get(s).getFirst() < next.seats().get(s).hands().getFirst().cards().size())
                    {
                        shown.hands.get(s).set(0, shown.hands.get(s).getFirst() + 1);
                        frames.add(new Frame(build(next, shown), false));
                    }
                }

                if (pass == 1 && next.dealerHiddenCards() > 0) shown.dealerHidden++;
                else shown.dealerVisible++;
                frames.add(new Frame(build(next, shown), false));
            }
        }

        /* Hits, doubles, and split cards, in seat order */
        for (int s = 0; s < next.seats().size(); s++)
        {
            List<RoundView.HandView> hands = next.seats().get(s).hands();
            for (int h = 0; h < hands.size(); h++)
            {
                while (shown.hands.get(s).get(h) < hands.get(h).cards().size())
                {
                    shown.hands.get(s).set(h, shown.hands.get(s).get(h) + 1);
                    frames.add(new Frame(build(next, shown), false));
                }
            }
        }

        /* Dealer flips the hole card, then draws */
        if (shown.dealerHidden > next.dealerHiddenCards())
        {
            shown.dealerHidden = next.dealerHiddenCards();
            shown.dealerVisible++;
            frames.add(new Frame(build(next, shown), true));
        }
        while (shown.dealerVisible < next.dealerCards().size())
        {
            shown.dealerVisible++;
            frames.add(new Frame(build(next, shown), false));
        }

        /* The last frame is the real view, so drop it: the caller draws that with totals and results */
        if (!frames.isEmpty()) frames.removeLast();
        return frames;
    }

    private static Shown startingPoint(final RoundView onScreen, final RoundView next)
    {
        Shown shown = new Shown();
        for (RoundView.SeatView seat : next.seats())
        {
            RoundView.SeatView before = findSeat(onScreen, seat.seat());
            List<Integer> counts = new ArrayList<>();
            for (int h = 0; h < seat.hands().size(); h++)
            {
                int target = seat.hands().get(h).cards().size();
                if (before != null && h < before.hands().size())
                {
                    counts.add(Math.min(before.hands().get(h).cards().size(), target));
                }
                else
                {
                    /* A hand that just appeared from a split already holds the card that moved over */
                    counts.add(h > 0 ? Math.min(1, target) : 0);
                }
            }
            shown.hands.add(counts);
        }

        if (onScreen != null)
        {
            shown.dealerVisible = onScreen.dealerCards().size();
            shown.dealerHidden = onScreen.dealerHiddenCards();
        }
        return shown;
    }

    private static RoundView build(final RoundView next, final Shown shown)
    {
        List<RoundView.SeatView> seats = new ArrayList<>();
        for (int s = 0; s < next.seats().size(); s++)
        {
            RoundView.SeatView seat = next.seats().get(s);
            List<RoundView.HandView> hands = new ArrayList<>();
            for (int h = 0; h < seat.hands().size(); h++)
            {
                RoundView.HandView hand = seat.hands().get(h);
                int count = shown.hands.get(s).get(h);
                if (count == 0) continue;

                boolean complete = count == hand.cards().size();
                hands.add(new RoundView.HandView(hand.cards().subList(0, count),
                        complete ? hand.total() : TableScreen.UNKNOWN_TOTAL, complete && hand.soft(),
                        hand.bet(), hand.state(), Hand.HandResult.IN_PROGRESS));
            }
            seats.add(new RoundView.SeatView(seat.seat(), seat.playerId(), seat.playerName(),
                    seat.bet(), seat.left(), hands, seat.activeHand()));
        }

        /* While the dealer is still showing cards, keep the total hidden */
        List<Card> dealerCards = next.dealerCards().subList(0, Math.min(shown.dealerVisible, next.dealerCards().size()));
        return new RoundView(next.phase(), dealerCards, shown.dealerHidden, null, seats, next.activeSeat());
    }

    private static RoundView.SeatView findSeat(final RoundView view, final int seat)
    {
        if (view == null) return null;
        for (RoundView.SeatView s : view.seats())
        {
            if (s.seat() == seat) return s;
        }
        return null;
    }
}
