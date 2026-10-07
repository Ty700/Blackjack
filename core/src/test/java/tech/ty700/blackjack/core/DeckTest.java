package tech.ty700.blackjack.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Spec:
 *  - Allowed deck counts: 1, 2, 4, 6, 8. Anything else falls back to 6.
 *  - Allowed shuffle counts: 1 through 8. Anything else falls back to 3.
 *  - Defaults: 6 decks, 3 shuffles.
 *  - A standard deck is 52 cards: one of every Rank in every Suit.
 *  - The shoe holds every card once per deck, in a shuffled order.
 *  - drawCard() draws one card. drawCards(n) draws n cards. cardsRemaining() says how many are left.
 *  - If more cards are asked for than are left, the shoe is rebuilt to a full fresh shoe first,
 *    then the cards are drawn from it.
 */
public class DeckTest {
    private static final int CARDS_PER_DECK = 52;
    private static final int DEFAULT_DECKS = 6;
    private static final int DEFAULT_SHUFFLES = 3;

    private static List<Card> drawAll(Deck deck)
    {
        /* Bounded loop, so a bug that refills the shoe can't hang the test */
        int remaining = deck.cardsRemaining();
        List<Card> drawn = new ArrayList<>();
        for(int i = 0; i < remaining; i++)
        {
            drawn.add(deck.drawCard()[0]);
        }
        return drawn;
    }

    /* ---------- Defaults ---------- */

    @Test
    void noArgConstructorUsesDefaults()
    {
        Deck deck = new Deck();
        assertEquals(DEFAULT_DECKS, deck.getNumDecks());
        assertEquals(DEFAULT_SHUFFLES, deck.getNumShuffles());
        assertEquals(DEFAULT_DECKS * CARDS_PER_DECK, deck.cardsRemaining());
    }

    @Test
    void oneArgConstructorUsesDefaultShuffles()
    {
        Deck deck = new Deck(2);
        assertEquals(2, deck.getNumDecks());
        assertEquals(DEFAULT_SHUFFLES, deck.getNumShuffles());
    }

    /* ---------- Deck count ---------- */

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 6, 8})
    void allowedDeckCountIsKept(int amountOfDecks)
    {
        Deck deck = new Deck(amountOfDecks);
        assertEquals(amountOfDecks, deck.getNumDecks());
        assertEquals(amountOfDecks * CARDS_PER_DECK, deck.cardsRemaining());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 3, 5, 7, 9, Integer.MAX_VALUE})
    void disallowedDeckCountFallsBackToDefault(int amountOfDecks)
    {
        Deck deck = new Deck(amountOfDecks);
        assertEquals(DEFAULT_DECKS, deck.getNumDecks());
        assertEquals(DEFAULT_DECKS * CARDS_PER_DECK, deck.cardsRemaining());
    }

    /* ---------- Shuffle count ---------- */

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
    void allowedShuffleCountIsKept(int amountOfShuffles)
    {
        assertEquals(amountOfShuffles, new Deck(1, amountOfShuffles).getNumShuffles());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 9, Integer.MAX_VALUE})
    void disallowedShuffleCountFallsBackToDefault(int amountOfShuffles)
    {
        assertEquals(DEFAULT_SHUFFLES, new Deck(1, amountOfShuffles).getNumShuffles());
    }

    @Test
    void badDeckCountDoesNotAffectGoodShuffleCount()
    {
        Deck deck = new Deck(-100, 2);
        assertEquals(DEFAULT_DECKS, deck.getNumDecks());
        assertEquals(2, deck.getNumShuffles());
    }

    @Test
    void badShuffleCountDoesNotAffectGoodDeckCount()
    {
        Deck deck = new Deck(1, -100);
        assertEquals(1, deck.getNumDecks());
        assertEquals(DEFAULT_SHUFFLES, deck.getNumShuffles());
    }

    /* ---------- Drawing ---------- */

    @Test
    void drawCardReturnsOneCard()
    {
        Card[] drawn = new Deck(1).drawCard();
        assertEquals(1, drawn.length);
        assertNotNull(drawn[0]);
    }

    @Test
    void drawCardRemovesOneCard()
    {
        Deck deck = new Deck(1);
        deck.drawCard();
        assertEquals(CARDS_PER_DECK - 1, deck.cardsRemaining());
    }

    @Test
    void drawCardsReturnsRequestedAmount()
    {
        Deck deck = new Deck(1);
        Card[] drawn = deck.drawCards(5);
        assertEquals(5, drawn.length);
        for(Card c: drawn)
        {
            assertNotNull(c);
        }
        assertEquals(CARDS_PER_DECK - 5, deck.cardsRemaining());
    }

    /* A single deck has 52 different cards, so any cards drawn from it must all be different */
    @Test
    void drawCardsFromOneDeckAreAllDifferent()
    {
        Card[] drawn = new Deck(1).drawCards(10);
        assertEquals(10, new HashSet<>(Arrays.asList(drawn)).size());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 6, 8})
    void canDrawEveryCardUntilEmpty(int amountOfDecks)
    {
        Deck deck = new Deck(amountOfDecks);
        List<Card> drawn = drawAll(deck);
        assertEquals(amountOfDecks * CARDS_PER_DECK, drawn.size());
        assertEquals(0, deck.cardsRemaining());
    }

    /* ---------- Rebuilding ---------- */

    @Test
    void drawingExactlyWhatIsLeftDoesNotRebuild()
    {
        Deck deck = new Deck(1);
        deck.drawCards(CARDS_PER_DECK - 3);
        deck.drawCards(3);
        assertEquals(0, deck.cardsRemaining());
    }

    @Test
    void drawingMoreThanIsLeftRebuildsThenDraws()
    {
        Deck deck = new Deck(1);
        deck.drawCards(CARDS_PER_DECK - 2);
        Card[] drawn = deck.drawCards(5);
        assertEquals(5, drawn.length);
        assertEquals(CARDS_PER_DECK - 5, deck.cardsRemaining());
    }

    @Test
    void drawingFromEmptyShoeRebuildsThenDraws()
    {
        Deck deck = new Deck(1);
        deck.drawCards(CARDS_PER_DECK);
        assertNotNull(deck.drawCard()[0]);
        assertEquals(CARDS_PER_DECK - 1, deck.cardsRemaining());
    }

    /* After a rebuild, the drawn cards plus what is left must be exactly one fresh shoe */
    @ParameterizedTest
    @ValueSource(ints = {1, 6})
    void rebuiltShoeHoldsEveryCardOncePerDeck(int amountOfDecks)
    {
        Deck deck = new Deck(amountOfDecks);
        deck.drawCards(amountOfDecks * CARDS_PER_DECK - 2);

        List<Card> cards = new ArrayList<>(Arrays.asList(deck.drawCards(5)));
        cards.addAll(drawAll(deck));

        assertEquals(amountOfDecks * CARDS_PER_DECK, cards.size());
        Map<Card, Integer> counts = new HashMap<>();
        for(Card c: cards)
        {
            counts.merge(c, 1, Integer::sum);
        }
        for(Suit s: Suit.values())
        {
            for(Rank r: Rank.values())
            {
                assertEquals(amountOfDecks, counts.getOrDefault(new Card(r, s), 0));
            }
        }
    }

    /* ---------- Shoe contents ---------- */

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 6, 8})
    void shoeHoldsEveryCardOncePerDeck(int amountOfDecks)
    {
        Map<Card, Integer> counts = new HashMap<>();
        for(Card c: drawAll(new Deck(amountOfDecks)))
        {
            assertNotNull(c);
            counts.merge(c, 1, Integer::sum);
        }

        assertEquals(CARDS_PER_DECK, counts.size());
        for(Suit s: Suit.values())
        {
            for(Rank r: Rank.values())
            {
                Card expected = new Card(r, s);
                assertEquals(amountOfDecks, counts.getOrDefault(expected, 0),
                        "Wrong count for " + expected.displayString());
            }
        }
    }

    /* ---------- Shuffling ---------- */

    /* An unshuffled shoe would come out in the same order every time.
     * Two real shuffles matching has a chance of about 1 in 52!, so this won't flake. */
    @ParameterizedTest
    @ValueSource(ints = {1, 6})
    void twoShoesComeOutInDifferentOrders(int amountOfDecks)
    {
        assertNotEquals(drawAll(new Deck(amountOfDecks)), drawAll(new Deck(amountOfDecks)));
    }
}
