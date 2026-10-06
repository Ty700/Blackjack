package tech.ty700.blackjack.core;

public enum Suit {
    HEART("♥", 1),
    SPADE("♠", 2),
    DIAMOND("♦", 3),
    CLUB("♣", 4);

    private final String symbol;
    private final int value;

    Suit(String aSymbol, int aValue)
    {
        this.symbol = aSymbol;
        this.value = aValue;
    }

    public String getSymbol() { return this.symbol; }
    public int getValue() { return this.value; }

}
