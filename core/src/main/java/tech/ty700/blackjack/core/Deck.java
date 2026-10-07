package tech.ty700.blackjack.core;

import java.util.*;

public class Deck {
    /* User passed args */
    private final int numDecks;
    private final int shuffleAmount;

    /* Used to build the shoe */
    private static final int[] validDeckAmounts = {1, 2, 4, 6, 8};

    /* Assert that validDeckAmounts is sorted least to greatest */
    private static final int MAX_AMOUNT_OF_DECKS = validDeckAmounts[validDeckAmounts.length - 1];

    /* Shoe that is used, build by individual decks */
    private List<Card> shoe = new ArrayList<>();

    /* Defaults */
    private static final int DEFAULT_AMOUNT_OF_DECKS    = 6;
    private static final int DEFAULT_AMOUNT_OF_SHUFFLES = 3;
    private static final int CARDS_PER_DECK             = 52;

    public Deck()
    {
        /* If aNumOfDecks isn't passed, then default amount of decks is DEFAULT_AMOUNT_OF_DECKS */
        /* if aNumOfShuffles isn't passed, then default amount of shuffles is DEFAULT_AMOUNT_OF_SHUFFLES */
        this(DEFAULT_AMOUNT_OF_DECKS, DEFAULT_AMOUNT_OF_SHUFFLES);
    }

    public Deck(int aNumOfDecks) {
        this(aNumOfDecks, DEFAULT_AMOUNT_OF_SHUFFLES);
    }

    public Deck(int aNumOfDecks, int aNumOfShuffles)
    {
        this.numDecks = normalizeDeckAmount(aNumOfDecks);
        this.shuffleAmount = normalizeShuffleAmount(aNumOfShuffles);

        /* Build deck */
        this.buildShoe();
    }

    /* A part of coding is setting up safeguards for the version of you in the futrue that
     * is dumb... and does something like negative decks or something idk... */
    private int normalizeDeckAmount(final int amountOfDecks)
    {
        if (Arrays.stream(validDeckAmounts).anyMatch(x -> x == amountOfDecks)) return amountOfDecks;
        return DEFAULT_AMOUNT_OF_DECKS;
    }

    private int normalizeShuffleAmount(final int amountOfShuffles)
    {
        return (amountOfShuffles < 1 || amountOfShuffles > 8) ? DEFAULT_AMOUNT_OF_SHUFFLES : amountOfShuffles;
    }

    /* For testing... don't really need these */
    public int getNumDecks()        { return this.numDecks; }
    public int getNumShuffles()     { return this.shuffleAmount; }
    /* Needed?? */
    public int cardsRemaining()    { return this.shoe.size(); }

    private void buildShoe()
    {
        /* Used for creating deck */
        List<List<Card>> individualDecks = Collections.synchronizedList(new ArrayList<>(Collections.nCopies(this.numDecks, null)));

        /* Designate individual threads to build the shoe */
        Thread[] threads = new Thread[this.numDecks];
        for(int i = 0; i < this.numDecks; i++)
        {
            final int idx = i;
            threads[i] = new Thread(() ->
            {
                List<Card> singleDeck = new ArrayList<>(CARDS_PER_DECK);
                for(Suit s: Suit.values())
                {
                    for(Rank r: Rank.values())
                    {
                        singleDeck.add(new Card(r, s));
                    }
                }
                individualDecks.set(idx, singleDeck);
            });
            threads[i].start();
        }

        /* Wait for all threads to be done building the decks */
        try{
            for(Thread t: threads)
            {
                t.join();
            }
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while joining the threads in buildDecks", e);
        }

        /* Add all the decks to the shoe */
        this.shoe.clear();
        for(List<Card> d: individualDecks)
        {
            this.shoe.addAll(d);
        }

        /* Shuffle */
        for(int i = 0; i < this.shuffleAmount; i++)
        {
            Collections.shuffle(this.shoe);
        }
    }

    /* Draw mechanism */
    /* Why this way? I only wanted to write draw logic once... bite me */
    public Card[] drawCard() { return this.drawCards(1); }
    public Card[] drawCards(final int numOfCards) {

        /* TODO: Need to alert user that the shoe was rebuilt */
        /* This will do for now though */
        if(numOfCards > this.cardsRemaining()) buildShoe();

        Card[] drawnCards = new Card[numOfCards];
        for(int i = 0; i < numOfCards; i++)
        {
            drawnCards[i] = this.shoe.removeLast();
        }
        return drawnCards;
    }
}
