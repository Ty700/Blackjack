package tech.ty700.blackjack.core;

public record Card(Rank rank, Suit suit) {
    public String displayString()
    {
        return rank.getSymbol() + suit.getSymbol();
    }
}
