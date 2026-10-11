package tech.ty700.blackjack.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * Spec: standard multi-deck basic strategy chart, dealer hits soft 17.
 * Pairs: always split A and 8, never 5 or 10. Hard 17+ stands, 11 doubles.
 * Soft 18 doubles vs 2-6, stands vs 7-8, hits vs 9-A.
 * When a double isn't allowed, fall back to the chart's hit or stand.
 */
public class BasicStrategyTest
{
    private static final Set<Action> ALL = EnumSet.allOf(Action.class);
    private static final Set<Action> NO_DOUBLE_OR_SPLIT = EnumSet.of(Action.HIT, Action.STAND);

    private static Action play(String ranks, String up, Set<Action> legal)
    {
        Hand hand = new Hand(HouseRules.MIN_BET);
        for (String r : ranks.split(" "))
        {
            hand.addCard(new Card(Rank.valueOf(r), Suit.HEART));
        }
        List<Card> cards = hand.getHand();
        return BasicStrategy.choose(cards, hand.getHandTotal(), hand.isSoft(), new Card(Rank.valueOf(up), Suit.CLUB), legal);
    }

    @ParameterizedTest(name = "{0} vs {1} -> {2}")
    @CsvSource({
            /* Hard totals */
            "TEN SEVEN,     ACE,    STAND",
            "TEN SIX,       SIX,    STAND",
            "TEN SIX,       SEVEN,  HIT",
            "TEN TWO,       THREE,  HIT",
            "TEN TWO,       FOUR,   STAND",
            "SIX FIVE,      ACE,    DOUBLE",
            "SIX FOUR,      NINE,   DOUBLE",
            "SIX FOUR,      TEN,    HIT",
            "FIVE FOUR,     TWO,    HIT",
            "FIVE FOUR,     THREE,  DOUBLE",
            "FIVE THREE,    SIX,    HIT",
            /* Soft totals */
            "ACE NINE,      SIX,    STAND",
            "ACE EIGHT,     SIX,    DOUBLE",
            "ACE EIGHT,     FIVE,   STAND",
            "ACE SEVEN,     FOUR,   DOUBLE",
            "ACE SEVEN,     SEVEN,  STAND",
            "ACE SEVEN,     NINE,   HIT",
            "ACE SIX,       THREE,  DOUBLE",
            "ACE SIX,       TWO,    HIT",
            "ACE TWO,       FIVE,   DOUBLE",
            "ACE TWO,       FOUR,   HIT",
            /* Pairs */
            "ACE ACE,       TEN,    SPLIT",
            "EIGHT EIGHT,   ACE,    SPLIT",
            "TEN TEN,       SIX,    STAND",
            "KING QUEEN,    FIVE,   STAND",
            "FIVE FIVE,     SIX,    DOUBLE",
            "NINE NINE,     SEVEN,  STAND",
            "NINE NINE,     EIGHT,  SPLIT",
            "SEVEN SEVEN,   SEVEN,  SPLIT",
            "SEVEN SEVEN,   EIGHT,  HIT",
            "FOUR FOUR,     FIVE,   SPLIT",
            "FOUR FOUR,     FOUR,   HIT",
            "TWO TWO,       SEVEN,  SPLIT",
    })
    void followsTheChart(String ranks, String up, Action expected)
    {
        assertEquals(expected, play(ranks, up, ALL));
    }

    @ParameterizedTest(name = "{0} vs {1} -> {2} without double/split")
    @CsvSource({
            "SIX FIVE,      SIX,    HIT",
            "ACE SEVEN,     FOUR,   STAND",
            "ACE EIGHT,     SIX,    STAND",
            "EIGHT EIGHT,   TEN,    HIT",
            "ACE ACE,       SIX,    HIT",
    })
    void fallsBackWhenDoubleOrSplitIsNotAllowed(String ranks, String up, Action expected)
    {
        assertEquals(expected, play(ranks, up, NO_DOUBLE_OR_SPLIT));
    }

    @Test
    void threeCardHandsNeverDouble()
    {
        assertEquals(Action.HIT, play("TWO THREE SIX", "SIX", NO_DOUBLE_OR_SPLIT));
    }

    @Test
    void standsOn21EvenThoughHittingIsAllowed()
    {
        assertEquals(Action.STAND, play("SEVEN SEVEN SEVEN", "TEN", NO_DOUBLE_OR_SPLIT));
    }
}
