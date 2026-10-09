package tech.ty700.blackjack.core;

public record Card(Rank rank, Suit suit) {
    public String displayString()
    {
        return this.rank.getSymbol() + this.suit.getSymbol();
    }

    public Rank getRank()
    {
        return this.rank();
    }
    
    public int getRankValue()
    {
        return this.rank.getValue();
    }

    public String getSuitSymbol()
    {
        return this.suit.getSymbol();
    }
}
