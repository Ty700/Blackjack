package tech.ty700.blackjack.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DeckTest {
    @Test
    void defaultEmptyDeck()
    {
        Deck deck = new Deck();
        assertEquals(6, deck.getNumDecks());
        assertEquals(3, deck.getNumShuffles());
    }

    @Test
    void defaultFilledDeck()
    {
        Deck deck = new Deck(1,2);
        assertEquals(1, deck.getNumDecks());
        assertEquals(2, deck.getNumShuffles());
    }

    @Test
    void negativeDeckCount()
    {
        Deck deck = new Deck(-100,2);
        assertEquals(6, deck.getNumDecks());
        assertEquals(2, deck.getNumShuffles());
    }

    @Test
    void negativeShuffleCount()
    {
        Deck deck = new Deck(1,-100);
        assertEquals(1, deck.getNumDecks());
        assertEquals(3, deck.getNumShuffles());
    }

    @Test
    void dumbDeck()
    {
        Deck deck = new Deck(-100, -100);
        assertEquals(6, deck.getNumDecks());
        assertEquals(3, deck.getNumShuffles());
    }
}
