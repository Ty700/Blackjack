package tech.ty700.blackjack.core;

import java.math.BigInteger;
import java.util.UUID;

/**
 * This class will be what the Dealer and Player class extends
 * Holds who someone is and their chips. Hands live in the Round, not here.
 */
public abstract class Participant {
    private final String name;
    private BigInteger networth;

    /* DB tracking */
    private final UUID id;

    Participant(final String aName, final UUID aUuid, final BigInteger aNetworth)
    {
        /* Name: Captured by game menu */
        /* UUID will be stored in a DB and we will grab their stored information from DB and assign it here */
        this.name = aName;
        this.id = aUuid;
        this.networth = aNetworth;
    }

    public UUID getUuid()           { return this.id; }
    public String getName()         { return this.name; }
    public BigInteger getNetworth() { return this.networth; }

    public boolean canAfford(final BigInteger amount)
    {
        return this.networth.compareTo(amount) >= 0;
    }

    /* Chips only move through core (the round, loans, prestige), so these stay package-private */
    void addChips(final BigInteger amount)
    {
        this.networth = this.networth.add(amount);
        onChipsChanged();
    }

    void takeChips(final BigInteger amount)
    {
        if (!canAfford(amount))
        {
            throw new IllegalStateException(this.name + " can't afford " + amount);
        }
        this.networth = this.networth.subtract(amount);
        onChipsChanged();
    }

    void setChips(final BigInteger amount)
    {
        this.networth = amount;
        onChipsChanged();
    }

    /* Hook for subclasses that track chip stats */
    protected void onChipsChanged() {}
}
