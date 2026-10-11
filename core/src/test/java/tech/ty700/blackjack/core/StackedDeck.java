package tech.ty700.blackjack.core;

import java.util.ArrayDeque;
import java.util.Deque;

/* A deck that deals a fixed list of cards in order, so a test knows every card in advance */
class StackedDeck extends Deck
{
    private final Deque<Card> cards = new ArrayDeque<>();

    StackedDeck(Rank... ranks)
    {
        super(1);
        add(ranks);
    }

    void add(Rank... ranks)
    {
        for (Rank r : ranks)
        {
            this.cards.add(new Card(r, Suit.SPADE));
        }
    }

    int undealt() { return this.cards.size(); }

    @Override
    public Card[] drawCards(int numOfCards)
    {
        Card[] drawn = new Card[numOfCards];
        for (int i = 0; i < numOfCards; i++)
        {
            if (this.cards.isEmpty()) throw new AssertionError("Test dealt more cards than it stacked");
            drawn[i] = this.cards.removeFirst();
        }
        return drawn;
    }

    /* Never trigger a reshuffle, the stack is the whole shoe */
    @Override
    public int cardsRemaining() { return 1_000; }

    @Override
    public void reshuffle() {}
}
