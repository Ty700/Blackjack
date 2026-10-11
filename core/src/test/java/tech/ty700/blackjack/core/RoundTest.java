package tech.ty700.blackjack.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static tech.ty700.blackjack.core.Rank.*;

/*
 * Spec (agreed design):
 *  - Deal: two passes, every betting player in seat order, then the dealer. The dealer's second card is the hole card.
 *  - Chips leave the player when a bet is placed. Win pays 1:1, push returns the bet, blackjack pays 2:1.
 *  - Bets are at least 100 in steps of 25, one bet per player per round. Players may sit out.
 *  - Dealer peeks: a dealer blackjack ends the round, player blackjacks push, everyone else loses the original bet.
 *  - A player blackjack is paid immediately.
 *  - Dealer hits soft 17. If every hand is already decided, the dealer does not draw.
 *  - A hand may hit on 21. Double: one card, doubled bet, blocked if the player can't afford it.
 *  - Split: up to 4 hands, split bet equals the original, split aces get one card each and can't be re-split,
 *    a split 21 counts as blackjack.
 *  - Only the player whose turn it is may act. Illegal actions throw.
 *  - Joining mid-round waits for the next round. Leaving mid-round stands the hands and still settles them.
 *    Leaving during betting returns the bet.
 *  - The dealer's hole card is hidden from the view until the dealer's turn.
 *  - Before dealing, the shoe is rebuilt if fewer than (betting players + dealer) * 6 cards are left.
 */
public class RoundTest
{
    private Dealer dealer;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp()
    {
        dealer = new Dealer("House");
        alice = new Player("Alice");
        bob = new Player("Bob");
    }

    private static BigInteger chips(long n) { return BigInteger.valueOf(n); }

    private Table table(Deck deck, Player... players)
    {
        Table t = new Table(dealer, deck);
        for (int i = 0; i < players.length; i++)
        {
            t.playerConnect(players[i], i);
        }
        return t;
    }

    private static Round betAndDeal(Table t, long bet, Player... bettors)
    {
        Round r = t.startRound();
        for (Player p : bettors)
        {
            r.placeBet(p, chips(bet));
        }
        r.closeBetting();
        return r;
    }

    /* ---------- Basic payouts ---------- */

    @Test
    void winPaysOneToOne()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, TEN, NINE, SEVEN), alice), 100, alice);
        r.apply(alice, Action.STAND);

        assertEquals(Round.Phase.DONE, r.getPhase());
        assertEquals(chips(1100), alice.getNetworth());
        assertEquals(chips(-100), dealer.getHouseProfit());
    }

    @Test
    void lossKeepsTheBet()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, TEN, SEVEN, NINE), alice), 100, alice);
        r.apply(alice, Action.STAND);

        assertEquals(chips(900), alice.getNetworth());
        assertEquals(chips(100), dealer.getHouseProfit());
    }

    @Test
    void pushReturnsTheBet()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, TEN, EIGHT, EIGHT), alice), 100, alice);
        r.apply(alice, Action.STAND);

        assertEquals(chips(1000), alice.getNetworth());
        assertEquals(BigInteger.ZERO, dealer.getHouseProfit());
    }

    @Test
    void chipsLeaveWhenTheBetIsPlaced()
    {
        Round r = table(new StackedDeck(), alice).startRound();
        r.placeBet(alice, chips(250));
        assertEquals(chips(750), alice.getNetworth());
    }

    /* ---------- Blackjack ---------- */

    @Test
    void playerBlackjackPaysTwoToOne()
    {
        Round r = betAndDeal(table(new StackedDeck(ACE, NINE, KING, SEVEN), alice), 100, alice);

        assertEquals(Round.Phase.DONE, r.getPhase());
        assertEquals(chips(1200), alice.getNetworth());
        assertEquals(chips(-200), dealer.getHouseProfit());
    }

    @Test
    void playerBlackjackIsPaidBeforeOtherPlayersAct()
    {
        /* Alice ACE KING, Bob TEN EIGHT, dealer NINE SEVEN, then the dealer draws TEN and busts */
        Round r = betAndDeal(table(new StackedDeck(ACE, TEN, NINE, KING, EIGHT, SEVEN, TEN), alice, bob), 100, alice, bob);

        assertEquals(Round.Phase.PLAYER_TURNS, r.getPhase());
        assertEquals(chips(1200), alice.getNetworth());

        r.apply(bob, Action.STAND);
        assertEquals(chips(1100), bob.getNetworth());
    }

    @Test
    void dealerBlackjackEndsTheRoundAndTakesOnlyTheOriginalBet()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, ACE, NINE, KING), alice), 100, alice);

        assertEquals(Round.Phase.DONE, r.getPhase());
        assertTrue(r.getLegalActions(alice).isEmpty());
        assertEquals(chips(900), alice.getNetworth());
        assertEquals(chips(100), dealer.getHouseProfit());
    }

    @Test
    void dealerBlackjackPushesPlayerBlackjack()
    {
        Round r = betAndDeal(table(new StackedDeck(ACE, ACE, KING, KING), alice), 100, alice);

        assertEquals(Round.Phase.DONE, r.getPhase());
        assertEquals(chips(1000), alice.getNetworth());
    }

    @Test
    void dealerAceWithoutBlackjackLetsPlayersAct()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, ACE, NINE, FIVE), alice), 100, alice);
        assertEquals(Round.Phase.PLAYER_TURNS, r.getPhase());
        assertTrue(r.getLegalActions(alice).contains(Action.STAND));
    }

    /* ---------- Dealer play ---------- */

    @Test
    void dealerHitsSoft17()
    {
        StackedDeck deck = new StackedDeck(TEN, ACE, EIGHT, SIX, TWO);
        Round r = betAndDeal(table(deck, alice), 100, alice);
        r.apply(alice, Action.STAND);

        assertEquals(3, r.getView().dealerCards().size());
        assertEquals(19, r.getView().dealerTotal());
        assertEquals(chips(900), alice.getNetworth());
    }

    @Test
    void dealerStandsOnHard17()
    {
        StackedDeck deck = new StackedDeck(TEN, TEN, EIGHT, SEVEN, FIVE);
        Round r = betAndDeal(table(deck, alice), 100, alice);
        r.apply(alice, Action.STAND);

        assertEquals(1, deck.undealt());
        assertEquals(chips(1100), alice.getNetworth());
    }

    @Test
    void dealerDoesNotDrawWhenEveryHandBusted()
    {
        StackedDeck deck = new StackedDeck(TEN, TEN, SIX, SIX, KING, FIVE);
        Round r = betAndDeal(table(deck, alice), 100, alice);
        r.apply(alice, Action.HIT);

        assertEquals(Round.Phase.DONE, r.getPhase());
        assertEquals(1, deck.undealt());
        assertEquals(chips(900), alice.getNetworth());
    }

    /* ---------- Hitting and doubling ---------- */

    @Test
    void canStillHitOn21()
    {
        Round r = betAndDeal(table(new StackedDeck(SEVEN, TEN, SEVEN, SEVEN, SEVEN), alice), 100, alice);
        r.apply(alice, Action.HIT);

        assertEquals(Round.Phase.PLAYER_TURNS, r.getPhase());
        assertTrue(r.getLegalActions(alice).contains(Action.HIT));
    }

    @Test
    void doubleDoublesTheBetAndTakesOneCard()
    {
        Round r = betAndDeal(table(new StackedDeck(FIVE, TEN, SIX, SEVEN, TEN), alice), 100, alice);
        r.apply(alice, Action.DOUBLE);

        assertEquals(Round.Phase.DONE, r.getPhase());
        RoundView.HandView hand = r.getView().seats().getFirst().hands().getFirst();
        assertEquals(3, hand.cards().size());
        assertEquals(chips(200), hand.bet());
        assertEquals(chips(1200), alice.getNetworth());
    }

    @Test
    void doubleIsBlockedWhenThePlayerCantAffordIt()
    {
        Player poor = new Player("Poor", UUID.randomUUID(), chips(150));
        Round r = betAndDeal(table(new StackedDeck(FIVE, TEN, SIX, SEVEN), poor), 100, poor);

        assertFalse(r.getLegalActions(poor).contains(Action.DOUBLE));
        assertThrows(IllegalStateException.class, () -> r.apply(poor, Action.DOUBLE));
        assertEquals(chips(50), poor.getNetworth());
    }

    /* ---------- Splitting ---------- */

    @Test
    void splitMakesTwoHandsWithTheSameBet()
    {
        /* Split eights: first hand gets TEN (18), second gets NINE (17). Dealer 17 */
        Round r = betAndDeal(table(new StackedDeck(EIGHT, TEN, EIGHT, SEVEN, TEN, NINE), alice), 100, alice);
        r.apply(alice, Action.SPLIT);

        RoundView.SeatView seat = r.getView().seats().getFirst();
        assertEquals(2, seat.hands().size());
        assertEquals(chips(100), seat.hands().get(0).bet());
        assertEquals(chips(100), seat.hands().get(1).bet());
        assertEquals(chips(800), alice.getNetworth());

        r.apply(alice, Action.STAND);
        r.apply(alice, Action.STAND);

        /* 18 wins 200, 17 pushes 100 */
        assertEquals(Round.Phase.DONE, r.getPhase());
        assertEquals(chips(1100), alice.getNetworth());
    }

    @Test
    void splitAcesGetOneCardEach()
    {
        /* A+5 (16) loses, A+6 (17) pushes against dealer 17 */
        Round r = betAndDeal(table(new StackedDeck(ACE, TEN, ACE, SEVEN, FIVE, SIX), alice), 100, alice);
        r.apply(alice, Action.SPLIT);

        assertEquals(Round.Phase.DONE, r.getPhase());
        assertEquals(chips(900), alice.getNetworth());
    }

    @Test
    void splitAcesCannotBeSplitAgain()
    {
        Round r = betAndDeal(table(new StackedDeck(ACE, TEN, ACE, SEVEN, ACE, ACE), alice), 100, alice);
        r.apply(alice, Action.SPLIT);

        assertEquals(Round.Phase.DONE, r.getPhase());
        assertEquals(2, r.getView().seats().getFirst().hands().size());
    }

    @Test
    void split21CountsAsBlackjack()
    {
        /* A+K pays 2:1 (300 back), A+5 loses to dealer 17 */
        Round r = betAndDeal(table(new StackedDeck(ACE, TEN, ACE, SEVEN, KING, FIVE), alice), 100, alice);
        r.apply(alice, Action.SPLIT);

        assertEquals(chips(1100), alice.getNetworth());
        assertEquals(Hand.HandResult.BLACKJACK, r.getView().seats().getFirst().hands().getFirst().result());
    }

    @Test
    void canResplitUpToFourHands()
    {
        StackedDeck deck = new StackedDeck(EIGHT, TEN, EIGHT, SEVEN);
        deck.add(EIGHT, EIGHT, EIGHT, EIGHT, EIGHT, EIGHT);
        Round r = betAndDeal(table(deck, alice), 100, alice);

        r.apply(alice, Action.SPLIT);
        r.apply(alice, Action.SPLIT);
        r.apply(alice, Action.SPLIT);

        assertEquals(4, r.getView().seats().getFirst().hands().size());
        assertFalse(r.getLegalActions(alice).contains(Action.SPLIT));
        assertTrue(r.getLegalActions(alice).contains(Action.HIT));
        assertEquals(chips(600), alice.getNetworth());
    }

    /* ---------- Turn order and legality ---------- */

    @Test
    void onlyThePlayerWhoseTurnItIsCanAct()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, TEN, TEN, NINE, NINE, SEVEN), alice, bob), 100, alice, bob);

        assertTrue(r.getLegalActions(bob).isEmpty());
        assertThrows(IllegalStateException.class, () -> r.apply(bob, Action.HIT));
    }

    @Test
    void illegalActionThrows()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, TEN, NINE, SEVEN), alice), 100, alice);
        assertThrows(IllegalStateException.class, () -> r.apply(alice, Action.SPLIT));
    }

    @Test
    void turnsGoInSeatOrder()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, TEN, TEN, NINE, NINE, SEVEN), alice, bob), 100, alice, bob);

        assertEquals(0, r.getView().activeSeat());
        r.apply(alice, Action.STAND);
        assertEquals(1, r.getView().activeSeat());
    }

    /* ---------- Betting ---------- */

    @Test
    void betsBelow100OrOffStepAreRejected()
    {
        Round r = table(new StackedDeck(), alice).startRound();
        assertThrows(IllegalArgumentException.class, () -> r.placeBet(alice, chips(50)));
        assertThrows(IllegalArgumentException.class, () -> r.placeBet(alice, chips(110)));
        assertEquals(chips(1000), alice.getNetworth());
    }

    @Test
    void betOffHundredButOnStepIsAccepted()
    {
        Round r = table(new StackedDeck(), alice).startRound();
        r.placeBet(alice, chips(125));
        assertEquals(chips(875), alice.getNetworth());
    }

    @Test
    void onlyOneBetPerRound()
    {
        Round r = table(new StackedDeck(), alice).startRound();
        r.placeBet(alice, chips(100));
        assertThrows(IllegalStateException.class, () -> r.placeBet(alice, chips(100)));
    }

    @Test
    void cannotBetMoreThanTheirChips()
    {
        Round r = table(new StackedDeck(), alice).startRound();
        assertThrows(IllegalStateException.class, () -> r.placeBet(alice, chips(1025)));
        assertEquals(chips(1000), alice.getNetworth());
    }

    @Test
    void playerWhoDoesNotBetSitsOut()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, TEN, NINE, SEVEN), alice, bob), 100, alice);

        RoundView.SeatView bobSeat = r.getView().seats().get(1);
        assertNull(bobSeat.bet());
        assertTrue(bobSeat.hands().isEmpty());
        assertEquals(chips(1000), bob.getNetworth());
    }

    @Test
    void roundWithNoBetsEndsWithoutDealing()
    {
        Round r = table(new StackedDeck(), alice).startRound();
        r.closeBetting();
        assertEquals(Round.Phase.DONE, r.getPhase());
    }

    @Test
    void cannotStartARoundWhileOneIsInProgress()
    {
        Table t = table(new StackedDeck(), alice);
        t.startRound();
        assertThrows(IllegalStateException.class, t::startRound);
    }

    /* ---------- Joining and leaving ---------- */

    @Test
    void playerWhoJoinsMidRoundWaitsForTheNextRound()
    {
        Player carol = new Player("Carol");
        Table t = table(new StackedDeck(TEN, TEN, NINE, SEVEN), alice);
        Round r = betAndDeal(t, 100, alice);

        t.playerConnect(carol, 3);
        assertFalse(r.isInRound(carol));
        assertEquals(1, r.getView().seats().size());

        r.apply(alice, Action.STAND);
        assertTrue(t.startRound().isInRound(carol));
    }

    @Test
    void playerWhoLeavesMidRoundStandsAndIsStillSettled()
    {
        Table t = table(new StackedDeck(TEN, TEN, NINE, SEVEN), alice);
        Round r = betAndDeal(t, 100, alice);

        t.playerDisconnect(alice);

        assertEquals(Round.Phase.DONE, r.getPhase());
        assertEquals(chips(1100), alice.getNetworth());
        assertTrue(t.isEmpty());
    }

    @Test
    void playerWhoLeavesDuringBettingGetsTheBetBack()
    {
        Table t = table(new StackedDeck(), alice);
        Round r = t.startRound();
        r.placeBet(alice, chips(100));

        t.playerDisconnect(alice);
        r.closeBetting();

        assertEquals(chips(1000), alice.getNetworth());
        assertEquals(Round.Phase.DONE, r.getPhase());
    }

    /* ---------- View ---------- */

    @Test
    void dealerHoleCardIsHiddenUntilTheDealersTurn()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, TEN, NINE, SEVEN), alice), 100, alice);

        RoundView during = r.getView();
        assertEquals(1, during.dealerCards().size());
        assertEquals(TEN, during.dealerCards().getFirst().rank());
        assertEquals(1, during.dealerHiddenCards());
        assertNull(during.dealerTotal());

        r.apply(alice, Action.STAND);

        RoundView after = r.getView();
        assertEquals(2, after.dealerCards().size());
        assertEquals(0, after.dealerHiddenCards());
        assertEquals(17, after.dealerTotal());
    }

    /* ---------- Reshuffle ---------- */

    @Test
    void shoeIsRebuiltWhenBelowTheEstimate()
    {
        /* One player plus the dealer needs 12 cards. 11 left means rebuild, then deal 4 */
        Deck deck = new Deck(1);
        deck.drawCards(52 - 11);
        betAndDeal(table(deck, alice), 100, alice);

        assertEquals(52 - 4, deck.cardsRemaining());
    }

    @Test
    void shoeIsKeptWhenAtTheEstimate()
    {
        Deck deck = new Deck(1);
        deck.drawCards(52 - 12);
        betAndDeal(table(deck, alice), 100, alice);

        assertEquals(12 - 4, deck.cardsRemaining());
    }

    /* ---------- Stats ---------- */

    @Test
    void settlementRecordsPlayerAndDealerStats()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, TEN, NINE, SEVEN), alice), 100, alice);
        r.apply(alice, Action.STAND);

        PlayerStats ps = alice.getStats();
        assertEquals(1, ps.getHandsPlayed());
        assertEquals(1, ps.getHandsWon());
        assertEquals(1, ps.getCurrentWinStreak());
        assertEquals(chips(100), ps.getBiggestWin());
        assertEquals(chips(100), ps.getBiggestBet());
        assertEquals(chips(100), ps.getTotalWagered());
        assertEquals(chips(1100), ps.getPeakChips());
        assertEquals(1, dealer.getStats().getHandsDealt());
        assertEquals(chips(100), dealer.getStats().getBiggestPayoutLost());
    }

    @Test
    void fiveCardHandCountsAsACharlie()
    {
        Round r = betAndDeal(table(new StackedDeck(TWO, TEN, THREE, SEVEN, TWO, THREE, FOUR), alice), 100, alice);
        r.apply(alice, Action.HIT);
        r.apply(alice, Action.HIT);
        r.apply(alice, Action.HIT);
        r.apply(alice, Action.STAND);

        assertEquals(1, alice.getStats().getFiveCardCharlies());
        assertEquals(1, alice.getStats().getHandsLost());
    }

    @Test
    void dealerBustIsRecorded()
    {
        Round r = betAndDeal(table(new StackedDeck(TEN, TEN, NINE, SIX, KING), alice), 100, alice);
        r.apply(alice, Action.STAND);

        assertEquals(1, dealer.getStats().getBusts());
        assertEquals(1, dealer.getStats().getLongestBustStreak());
    }

    @Test
    void blackjackAndBustAreRecorded()
    {
        betAndDeal(table(new StackedDeck(ACE, NINE, KING, SEVEN), alice), 100, alice);
        assertEquals(1, alice.getStats().getBlackjacks());

        Round r = betAndDeal(table(new StackedDeck(TEN, TEN, SIX, SEVEN, KING), bob), 100, bob);
        r.apply(bob, Action.HIT);
        assertEquals(1, bob.getStats().getBusts());
    }
}
