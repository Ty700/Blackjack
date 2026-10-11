package tech.ty700.blackjack.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * One hand of blackjack at a table, from bets to payouts. A new Round is made for every hand.
 *
 * State machine: BETTING, DEALING, DEALER_PEEK, PLAYER_TURNS, DEALER_TURN, SETTLEMENT, DONE.
 * The round only stops where a person has to decide (BETTING and PLAYER_TURNS).
 * Everything else runs on its own, so after any call the phase is BETTING, PLAYER_TURNS, or DONE.
 *
 * core never waits for input: the outside asks what's legal, then calls placeBet / closeBetting / apply.
 */
public class Round
{
    public enum Phase { BETTING, DEALING, DEALER_PEEK, PLAYER_TURNS, DEALER_TURN, SETTLEMENT, DONE }

    private final Dealer dealer;
    private final Deck deck;
    private final HouseLedger ledger;
    /* Only the players seated when the round started. Anyone who sits down later waits for the next round */
    private final List<Seat> seats = new ArrayList<>();
    private final Hand dealerHand = new Hand();

    private Phase phase = Phase.BETTING;
    private int activeSeat = 0;
    private boolean dealerBlackjack = false;
    private boolean dealerPlayed = false;

    private static final class Seat
    {
        final int seatNumber;
        final Player player;
        final List<SeatHand> hands = new ArrayList<>();
        BigInteger bet = null;
        int activeHand = 0;
        boolean left = false;
        /* Chips this player put in and got back this round */
        BigInteger staked = BigInteger.ZERO;
        BigInteger paid = BigInteger.ZERO;

        Seat(final int aSeatNumber, final Player aPlayer)
        {
            this.seatNumber = aSeatNumber;
            this.player = aPlayer;
        }

        boolean isBetting() { return this.bet != null; }
    }

    private static final class SeatHand
    {
        final Hand hand;
        boolean doubled = false;
        boolean settled = false;

        SeatHand(final Hand aHand) { this.hand = aHand; }
    }

    Round(final SortedMap<Integer, Player> seated, final Dealer aDealer, final Deck aDeck, final HouseLedger aLedger)
    {
        this.dealer = aDealer;
        this.deck = aDeck;
        this.ledger = aLedger;
        for (Map.Entry<Integer, Player> e : seated.entrySet())
        {
            this.seats.add(new Seat(e.getKey(), e.getValue()));
        }
    }

    public Phase getPhase()     { return this.phase; }
    public boolean isOver()     { return this.phase == Phase.DONE; }

    public boolean isInRound(final Player p)    { return findSeat(p) != null; }

    public boolean hasBet(final Player p)
    {
        Seat s = findSeat(p);
        return s != null && s.isBetting();
    }

    /* ---------- Betting ---------- */

    /* Chips leave the player as soon as the bet is placed */
    public void placeBet(final Player p, final BigInteger amount)
    {
        requirePhase(Phase.BETTING);
        Seat s = requireSeat(p);
        if (s.isBetting())
        {
            throw new IllegalStateException(p.getName() + " already bet this round");
        }
        if (!HouseRules.isValidBet(amount))
        {
            throw new IllegalArgumentException("Bet must be at least " + HouseRules.MIN_BET
                    + " and a multiple of " + HouseRules.BET_INCREMENT);
        }

        p.takeChips(amount);
        s.staked = s.staked.add(amount);
        s.bet = amount;
    }

    /* Only during the betting window, before betting, and only when broke */
    public void declareBankruptcy(final Player p)
    {
        requirePhase(Phase.BETTING);
        Seat s = requireSeat(p);
        if (s.isBetting())
        {
            throw new IllegalStateException(p.getName() + " already bet this round");
        }

        p.declareBankruptcy();
        this.dealer.getStats().recordPlayerBankrupted();
    }

    /* The server's betting timer ran out (or everyone bet). Deals and runs until someone has to act */
    public void closeBetting()
    {
        requirePhase(Phase.BETTING);
        List<Seat> playing = playing();
        if (playing.isEmpty())
        {
            this.phase = Phase.DONE;
            return;
        }

        for (Seat s : playing)
        {
            s.player.getStats().recordOpeningBet(s.bet);
            s.player.getStats().recordWager(s.bet);
            s.player.recordRoundBet();
        }

        deal(playing);
        peek();
    }

    /* ---------- Player turns ---------- */

    /* Empty unless it's this player's turn */
    public Set<Action> getLegalActions(final Player p)
    {
        if (this.phase != Phase.PLAYER_TURNS) return Set.of();

        Seat s = this.seats.get(this.activeSeat);
        if (!s.player.getUuid().equals(p.getUuid())) return Set.of();

        Hand h = s.hands.get(s.activeHand).hand;
        boolean canAffordAnotherBet = p.canAfford(h.getBetTotal());

        EnumSet<Action> legal = EnumSet.of(Action.STAND);
        if (h.canHit()) legal.add(Action.HIT);
        if (h.canDouble() && canAffordAnotherBet) legal.add(Action.DOUBLE);
        if (h.canSplit() && s.hands.size() < HouseRules.MAX_HANDS && canAffordAnotherBet) legal.add(Action.SPLIT);
        return Collections.unmodifiableSet(legal);
    }

    /* Anything not in getLegalActions throws, so a modified client can't cheat */
    public void apply(final Player p, final Action action)
    {
        if (!getLegalActions(p).contains(action))
        {
            throw new IllegalStateException(action + " is not allowed for " + p.getName() + " right now");
        }

        Seat s = this.seats.get(this.activeSeat);
        SeatHand sh = s.hands.get(s.activeHand);
        switch (action)
        {
            case HIT -> sh.hand.addCard(draw());
            case STAND -> sh.hand.stand();
            case DOUBLE ->
            {
                stake(s, sh.hand.getBetTotal());
                sh.doubled = true;
                sh.hand.doubleHand(draw());
            }
            case SPLIT -> split(s, sh);
        }

        advance();
    }

    /* ---------- Leaving ---------- */

    /* Betting: the bet comes back. Playing: their hands stand and still get settled */
    void playerLeft(final Player p)
    {
        Seat s = findSeat(p);
        if (s == null || this.phase == Phase.DONE) return;
        s.left = true;

        if (this.phase == Phase.BETTING)
        {
            if (s.isBetting())
            {
                p.addChips(s.bet);
                s.staked = s.staked.subtract(s.bet);
                s.bet = null;
            }
            return;
        }

        if (this.phase == Phase.PLAYER_TURNS && this.seats.indexOf(s) == this.activeSeat)
        {
            advance();
        }
    }

    /* ---------- View ---------- */

    public RoundView getView()
    {
        boolean revealed = this.phase == Phase.DEALER_TURN || this.phase == Phase.SETTLEMENT || this.phase == Phase.DONE;
        List<Card> dealerCards = this.dealerHand.getHand();
        List<Card> visible = revealed || dealerCards.isEmpty() ? dealerCards : dealerCards.subList(0, 1);

        List<RoundView.SeatView> seatViews = new ArrayList<>();
        for (int i = 0; i < this.seats.size(); i++)
        {
            Seat s = this.seats.get(i);
            List<RoundView.HandView> handViews = new ArrayList<>();
            for (SeatHand sh : s.hands)
            {
                Hand h = sh.hand;
                handViews.add(new RoundView.HandView(h.getHand(), h.getHandTotal(), h.isSoft(),
                        h.getBetTotal(), h.getHandState(), h.getHandResult()));
            }
            Integer activeHand = isActive(i) ? s.activeHand : null;
            seatViews.add(new RoundView.SeatView(s.seatNumber, s.player.getUuid(), s.player.getName(),
                    s.bet, s.left, List.copyOf(handViews), activeHand));
        }

        Integer activeSeatNumber = this.phase == Phase.PLAYER_TURNS ? this.seats.get(this.activeSeat).seatNumber : null;
        return new RoundView(this.phase, List.copyOf(visible), dealerCards.size() - visible.size(),
                revealed ? this.dealerHand.getHandTotal() : null, List.copyOf(seatViews), activeSeatNumber);
    }

    /* ---------- Internals ---------- */

    private void deal(final List<Seat> playing)
    {
        this.phase = Phase.DEALING;

        /* Reshuffle between rounds, never mid-hand (the deck's own rebuild is only a fallback) */
        int cardsNeeded = (playing.size() + 1) * HouseRules.CARDS_PER_HAND_ESTIMATE;
        if (this.deck.cardsRemaining() < cardsNeeded)
        {
            this.deck.reshuffle();
        }

        for (Seat s : playing)
        {
            s.hands.add(new SeatHand(new Hand(s.bet)));
        }

        /* Two passes: every player, then the dealer */
        for (int pass = 0; pass < 2; pass++)
        {
            for (Seat s : playing)
            {
                s.hands.getFirst().hand.addCard(draw());
            }
            this.dealerHand.addCard(draw());
        }
    }

    private void peek()
    {
        this.phase = Phase.DEALER_PEEK;

        /* Only an ace or 10-value up card can hide a blackjack, and the hand already knows if it is one */
        this.dealerBlackjack = this.dealerHand.getHandResult() == Hand.HandResult.BLACKJACK;
        if (this.dealerBlackjack)
        {
            settle();
            return;
        }

        /* Player blackjacks are paid right away */
        for (Seat s : playing())
        {
            SeatHand first = s.hands.getFirst();
            if (first.hand.getHandResult() == Hand.HandResult.BLACKJACK)
            {
                settleHand(s, first);
            }
        }

        this.phase = Phase.PLAYER_TURNS;
        this.activeSeat = 0;
        advance();
    }

    /* Moves to the next hand that needs a decision. Hands of players who left stand on their own */
    private void advance()
    {
        for (; this.activeSeat < this.seats.size(); this.activeSeat++)
        {
            Seat s = this.seats.get(this.activeSeat);
            for (; s.activeHand < s.hands.size(); s.activeHand++)
            {
                Hand h = s.hands.get(s.activeHand).hand;
                if (h.getHandState() != Hand.HandState.IN_PROGRESS) continue;
                if (!s.left) return;
                h.stand();
            }
        }

        dealerTurn();
    }

    private void split(final Seat s, final SeatHand sh)
    {
        stake(s, sh.hand.getBetTotal());
        boolean aces = sh.hand.getHand().getFirst().rank() == Rank.ACE;

        SeatHand other = new SeatHand(sh.hand.split());
        s.hands.add(s.activeHand + 1, other);
        sh.hand.addCard(draw());
        other.hand.addCard(draw());
        s.player.getStats().recordSplit();

        /* Split aces get one card each, so they can't be split again either */
        if (aces)
        {
            standIfLive(sh.hand);
            standIfLive(other.hand);
        }
    }

    private void dealerTurn()
    {
        this.phase = Phase.DEALER_TURN;

        /* If every hand is already decided (busted or paid blackjack), the dealer just flips the hole card */
        this.dealerPlayed = playing().stream()
                .flatMap(s -> s.hands.stream())
                .anyMatch(sh -> !sh.settled && sh.hand.getHandResult() != Hand.HandResult.BUSTED);

        if (this.dealerPlayed)
        {
            while (this.dealerHand.getHandState() == Hand.HandState.IN_PROGRESS && this.dealer.shouldHit(this.dealerHand))
            {
                this.dealerHand.addCard(draw());
            }
        }
        standIfLive(this.dealerHand);

        settle();
    }

    private void settle()
    {
        this.phase = Phase.SETTLEMENT;
        standIfLive(this.dealerHand);

        BigInteger houseNet = BigInteger.ZERO;
        for (Seat s : playing())
        {
            for (SeatHand sh : s.hands)
            {
                if (!sh.settled) settleHand(s, sh);
            }

            BigInteger repaid = s.player.autoRepay(s.paid.subtract(s.staked));
            this.ledger.add(repaid);
            houseNet = houseNet.add(s.staked).subtract(s.paid);
        }

        this.dealer.addChips(houseNet);
        boolean dealerBusted = this.dealerHand.getHandResult() == Hand.HandResult.BUSTED;
        this.dealer.getStats().recordRound(houseNet, this.dealerBlackjack, this.dealerPlayed, dealerBusted);

        this.phase = Phase.DONE;
    }

    private void settleHand(final Seat s, final SeatHand sh)
    {
        Hand h = sh.hand;
        BigInteger bet = h.getBetTotal();
        boolean blackjack = h.getHandResult() == Hand.HandResult.BLACKJACK;

        Hand.HandResult outcome;
        BigInteger payout;
        if (this.dealerBlackjack)
        {
            outcome = blackjack ? Hand.HandResult.PUSHED : Hand.HandResult.LOST;
            payout = blackjack ? bet : BigInteger.ZERO;
        }
        else if (h.getHandResult() == Hand.HandResult.BUSTED)
        {
            outcome = Hand.HandResult.BUSTED;
            payout = BigInteger.ZERO;
        }
        else if (blackjack)
        {
            outcome = Hand.HandResult.BLACKJACK;
            payout = bet.add(bet.multiply(BigInteger.valueOf(s.player.getPayoutMultiplier())));
        }
        else
        {
            boolean dealerBusted = this.dealerHand.getHandResult() == Hand.HandResult.BUSTED;
            int compare = Integer.compare(h.getHandTotal(), this.dealerHand.getHandTotal());
            if (dealerBusted || compare > 0)
            {
                outcome = Hand.HandResult.WON;
                payout = bet.add(bet);
            }
            else if (compare == 0)
            {
                outcome = Hand.HandResult.PUSHED;
                payout = bet;
            }
            else
            {
                outcome = Hand.HandResult.LOST;
                payout = BigInteger.ZERO;
            }
        }

        /* A dealer blackjack settles hands nobody got to play */
        standIfLive(h);
        s.player.getStats().recordHand(h, outcome, payout, sh.doubled);
        if (outcome != h.getHandResult()) h.setHandResult(outcome);
        sh.settled = true;

        if (payout.signum() > 0)
        {
            s.player.addChips(payout);
            s.paid = s.paid.add(payout);
        }
        this.dealer.getStats().recordPlayerWin(payout.subtract(bet));
    }

    private void stake(final Seat s, final BigInteger amount)
    {
        s.player.takeChips(amount);
        s.staked = s.staked.add(amount);
        s.player.getStats().recordWager(amount);
    }

    private static void standIfLive(final Hand h)
    {
        if (h.getHandState() == Hand.HandState.IN_PROGRESS) h.stand();
    }

    private Card draw()
    {
        return this.deck.drawCard()[0];
    }

    private List<Seat> playing()
    {
        return this.seats.stream().filter(Seat::isBetting).toList();
    }

    private boolean isActive(final int seatIndex)
    {
        return this.phase == Phase.PLAYER_TURNS && seatIndex == this.activeSeat;
    }

    private Seat findSeat(final Player p)
    {
        for (Seat s : this.seats)
        {
            if (s.player.getUuid().equals(p.getUuid())) return s;
        }
        return null;
    }

    private Seat requireSeat(final Player p)
    {
        Seat s = findSeat(p);
        if (s == null || s.left)
        {
            throw new IllegalStateException(p.getName() + " is not in this round");
        }
        return s;
    }

    private void requirePhase(final Phase expected)
    {
        if (this.phase != expected)
        {
            throw new IllegalStateException("Round is in " + this.phase + ", not " + expected);
        }
    }
}
