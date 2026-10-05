package tech.ty700.blackjack.core;

/**
 * This class will be what the Dealer and Player class extends
 * Defines basic functionality of a participant in Blackjack
 */
public abstract class Participant {
    public String name;
    public long networth;

    Participant(final String aName)
    {
        /* Name: Captured by game menu */
        /* Network: TBD*/
        this.name = aName;
    };

    public abstract int hit();
}
