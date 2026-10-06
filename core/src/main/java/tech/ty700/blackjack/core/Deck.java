package tech.ty700.blackjack.core;

public class Deck {
    private final int numDecks;
    private final int shuffleAmount;
    private Card[] deck;

    private static final int DEFAULT_AMOUNT_OF_DECKS = 6;
    private static final int DEFAULT_AMOUNT_OF_SHUFFLES = 3;

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
    }

    /* A part of coding is setting up safeguards for the version of you in the futrue that
     * is dumb... and does something like negative decks or something idk... */
    private int normalizeDeckAmount(final int amountOfDecks)
    {
        return (amountOfDecks < 1 ) ? DEFAULT_AMOUNT_OF_DECKS : amountOfDecks;
    }

    /* Almost forgot this one */
    private int normalizeShuffleAmount(final int amountOfShuffles)
    {
        return (amountOfShuffles < 1 ) ? DEFAULT_AMOUNT_OF_SHUFFLES : amountOfShuffles;
    }

    /* For testing... don't really need these */
    public int getNumDecks() { return this.numDecks; }
    public int getNumShuffles() { return this.shuffleAmount; }
}
