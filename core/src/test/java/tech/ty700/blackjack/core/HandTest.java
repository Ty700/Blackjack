package tech.ty700.blackjack.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Spec (standard blackjack, plus this game's house rules):
 *  - Number cards count face value. J/Q/K count 10.
 *  - An ace counts 11, unless that would bust the hand, then it counts 1.
 *  - Ace + any 10-value card as the first two cards is a blackjack, and the hand is finished.
 *  - Over 21 is a bust, and the hand is finished.
 *  - A hand may still hit on 21 (house rule), unless it is a blackjack.
 *  - Split: first two cards only, same value. A 10 only splits with a 10. J/Q/K split with each other.
 *  - Bets are positive multiples of 100.
 *  - Double: first two cards only, doubles the bet, takes exactly one card, then the hand is finished.
 *    A power-up may add a different (positive) amount instead of the original bet.
 *  - A finished hand takes no more cards.
 */
public class HandTest {
    private static final BigInteger BET = BigInteger.valueOf(100);

    /* Deals the ranks (space separated, e.g. "ACE KING") into a new player hand */
    private static Hand deal(String ranks)
    {
        Hand hand = new Hand(BET);
        for(String r: ranks.split(" "))
        {
            hand.addCard(new Card(Rank.valueOf(r), Suit.HEART));
        }
        return hand;
    }

    /* ---------- Totals ---------- */

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource({
            "TWO THREE,          5",
            "TEN SEVEN,          17",
            "KING QUEEN,         20",
            "JACK FIVE SIX,      21",
            "FIVE SIX TEN,       21",
            "TWO THREE FOUR FIVE, 14",
    })
    void hardTotals(String ranks, int total)
    {
        assertEquals(total, deal(ranks).getHandTotal());
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource({
            "ACE SIX,            17",   // soft 17
            "ACE SIX TEN,        17",   // ace drops to 1
            "ACE FIVE NINE,      15",   // ace drops to 1 after the third card
            "ACE ACE,            12",   // one ace 11, one ace 1
            "ACE ACE NINE,       21",
            "ACE ACE ACE ACE,    14",
            "TEN SIX ACE ACE,    18",   // both aces count 1
            "ACE TWO ACE SEVEN,  21",   // first ace 11, second 1
    })
    void softTotals(String ranks, int total)
    {
        assertEquals(total, deal(ranks).getHandTotal());
    }

    /* ---------- Blackjack ---------- */

    @ParameterizedTest(name = "{0}")
    @CsvSource({ "ACE TEN", "ACE JACK", "ACE QUEEN", "ACE KING", "TEN ACE", "KING ACE" })
    void aceAndTenValueIsBlackjack(String ranks)
    {
        Hand hand = deal(ranks);
        assertEquals(21, hand.getHandTotal());
        assertEquals(Hand.HandResult.BLACKJACK, hand.getHandResult());
        assertEquals(Hand.HandState.DONE, hand.getHandState());
        assertFalse(hand.canHit());
    }

    @Test
    void threeCard21IsNotBlackjack()
    {
        Hand hand = deal("SEVEN SEVEN SEVEN");
        assertEquals(21, hand.getHandTotal());
        assertNotEquals(Hand.HandResult.BLACKJACK, hand.getHandResult());
    }

    @Test
    void canStillHitOn21WhenNotBlackjack()
    {
        assertTrue(deal("SEVEN SEVEN SEVEN").canHit());
    }

    @Test
    void blackjackTakesNoMoreCards()
    {
        Hand hand = deal("ACE KING");
        assertThrows(IllegalStateException.class, () -> hand.addCard(new Card(Rank.TWO, Suit.CLUB)));
    }

    /* ---------- Bust ---------- */

    @ParameterizedTest(name = "{0}")
    @CsvSource({ "KING QUEEN TWO", "TEN SIX SEVEN", "NINE EIGHT FIVE" })
    void over21IsBust(String ranks)
    {
        Hand hand = deal(ranks);
        assertTrue(hand.getHandTotal() > 21);
        assertEquals(Hand.HandResult.BUSTED, hand.getHandResult());
        assertEquals(Hand.HandState.DONE, hand.getHandState());
        assertFalse(hand.canHit());
    }

    @Test
    void softHandDoesNotBustWhenAceCanDrop()
    {
        Hand hand = deal("ACE SIX TEN");
        assertNotEquals(Hand.HandResult.BUSTED, hand.getHandResult());
        assertTrue(hand.canHit());
    }

    @Test
    void bustedHandTakesNoMoreCards()
    {
        Hand hand = deal("KING QUEEN TWO");
        assertThrows(IllegalStateException.class, () -> hand.addCard(new Card(Rank.TWO, Suit.CLUB)));
    }

    /* ---------- Standing ---------- */

    @Test
    void standFinishesTheHand()
    {
        Hand hand = deal("TEN SEVEN");
        hand.stand();
        assertEquals(Hand.HandState.DONE, hand.getHandState());
        assertFalse(hand.canHit());
        assertEquals(17, hand.getHandTotal());
    }

    @Test
    void stoodHandTakesNoMoreCards()
    {
        Hand hand = deal("TEN SEVEN");
        hand.stand();
        assertThrows(IllegalStateException.class, () -> hand.addCard(new Card(Rank.TWO, Suit.CLUB)));
    }

    /* ---------- Splitting ---------- */

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "EIGHT EIGHT,   true",
            "ACE ACE,       true",
            "TWO TWO,       true",
            "TEN TEN,       true",
            "KING QUEEN,    true",
            "JACK KING,     true",
            "QUEEN JACK,    true",
            "KING TEN,      false",
            "TEN JACK,      false",
            "EIGHT NINE,    false",
            "FOUR FIVE,     false",
    })
    void splitPairs(String ranks, boolean canSplit)
    {
        assertEquals(canSplit, deal(ranks).canSplit());
    }

    @Test
    void cannotSplitAfterThirdCard()
    {
        assertFalse(deal("TWO TWO TWO").canSplit());
    }

    @Test
    void cannotSplitAfterStanding()
    {
        Hand hand = deal("EIGHT EIGHT");
        hand.stand();
        assertFalse(hand.canSplit());
    }

    /* ---------- Doubling ---------- */

    @Test
    void doubleDoublesBetTakesOneCardAndFinishes()
    {
        Hand hand = deal("FIVE SIX");
        hand.doubleHand(new Card(Rank.NINE, Suit.CLUB));
        assertEquals(BET.multiply(BigInteger.TWO), hand.getBetTotal());
        assertEquals(3, hand.getHand().size());
        assertEquals(20, hand.getHandTotal());
        assertEquals(Hand.HandState.DONE, hand.getHandState());
        assertFalse(hand.canHit());
    }

    @Test
    void cannotDoubleAfterThirdCard()
    {
        Hand hand = deal("TWO THREE FOUR");
        assertThrows(IllegalStateException.class, () -> hand.doubleHand(new Card(Rank.FIVE, Suit.CLUB)));
        assertEquals(BET, hand.getBetTotal());
    }

    @Test
    void cannotDoubleOnBlackjack()
    {
        Hand hand = deal("ACE KING");
        assertThrows(IllegalStateException.class, () -> hand.doubleHand(new Card(Rank.FIVE, Suit.CLUB)));
        assertEquals(BET, hand.getBetTotal());
    }

    @Test
    void doubleWithCustomAmountAddsThatAmount()
    {
        Hand hand = deal("FIVE SIX");
        hand.doubleHand(new Card(Rank.NINE, Suit.CLUB), BigInteger.valueOf(300));
        assertEquals(BigInteger.valueOf(400), hand.getBetTotal());
        assertEquals(Hand.HandState.DONE, hand.getHandState());
    }

    @ParameterizedTest(name = "additional bet {0}")
    @ValueSource(longs = {0, -100})
    void doubleRejectsNonPositiveAmount(long amount)
    {
        Hand hand = deal("FIVE SIX");
        assertThrows(IllegalArgumentException.class,
                () -> hand.doubleHand(new Card(Rank.NINE, Suit.CLUB), BigInteger.valueOf(amount)));
        assertEquals(BET, hand.getBetTotal());
        assertEquals(2, hand.getHand().size());
    }

    /* ---------- Bets ---------- */

    @ParameterizedTest(name = "bet {0}")
    @ValueSource(longs = {100, 200, 500, 1_000_000})
    void betMultipleOf100IsAccepted(long bet)
    {
        assertEquals(BigInteger.valueOf(bet), new Hand(BigInteger.valueOf(bet)).getBetTotal());
    }

    @ParameterizedTest(name = "bet {0}")
    @ValueSource(longs = {0, -100, 1, 50, 99, 150, 1_001})
    void betNotPositiveMultipleOf100IsRejected(long bet)
    {
        assertThrows(IllegalArgumentException.class, () -> new Hand(BigInteger.valueOf(bet)));
    }

    /* Chips go up to 100 vigintillion (10^65), far past what a long can hold */
    @Test
    void hugeBetIsAccepted()
    {
        BigInteger hundredVigintillion = BigInteger.TEN.pow(65);
        assertEquals(hundredVigintillion, new Hand(hundredVigintillion).getBetTotal());
    }
}
