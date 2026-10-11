package tech.ty700.blackjack.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * Spec: the dealer hits soft 17 (H17). Below 17 hit, soft 17 hit, hard 17 or more stand.
 */
public class DealerTest
{
    private static Hand dealerHand(String ranks)
    {
        Hand hand = new Hand();
        for (String r : ranks.split(" "))
        {
            hand.addCard(new Card(Rank.valueOf(r), Suit.CLUB));
        }
        return hand;
    }

    @ParameterizedTest(name = "{0} -> hit={1}")
    @CsvSource({
            "TEN SIX,           true",    // 16
            "ACE FIVE,          true",    // soft 16
            "ACE SIX,           true",    // soft 17
            "ACE TWO FOUR,      true",    // soft 17, three cards
            "TEN SEVEN,         false",   // hard 17
            "TEN SIX ACE,       false",   // hard 17, ace counts 1
            "ACE SEVEN,         false",   // soft 18
            "TEN EIGHT,         false",
    })
    void hitsSoft17(String ranks, boolean hits)
    {
        assertEquals(hits, new Dealer("House").shouldHit(dealerHand(ranks)));
    }
}
