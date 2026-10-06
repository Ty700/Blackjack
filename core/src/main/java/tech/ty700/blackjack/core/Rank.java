package tech.ty700.blackjack.core;

public enum Rank {
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("10", 10),
    JACK("J", 10),
    QUEEN("Q", 10),
    KING("K", 10),
    ACE("A", 11);

    private final String symbol;
    private final int value;

    Rank(String aSymbol, int aValue)
    {
        this.symbol = aSymbol;
        this.value = aValue;
    }

    public String getSymbol() { return this.symbol; }
    public int getValue() { return this.value; }
}
