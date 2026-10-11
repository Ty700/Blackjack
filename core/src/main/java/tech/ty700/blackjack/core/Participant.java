package tech.ty700.blackjack.core;

import java.math.BigInteger;
import java.util.UUID;

/**
 * This class will be what the Dealer and Player class extends
 * Defines basic functionality of a participant in Blackjack
 */
public abstract class Participant {
    private final static BigInteger STARTING_AMOUNT_CHIPS   = BigInteger.TEN.pow(3);
    private final static int STARTING_AMOUNT_POWERUPS       = 3;

    private final String name;
    private BigInteger networth;
    private Hand hand = new Hand();

    /* DB tracking */
    private final UUID id;

    /* New player */
    Participant(final String aName)
    {
        this(aName, UUID.randomUUID(), STARTING_AMOUNT_CHIPS);
    };

    /* Existing Player */
    Participant(final String aName, final UUID aUuid)
    {
        this(aName, aUuid, STARTING_AMOUNT_CHIPS);
    };

    /* Dealer */
    Participant(final String aName, final UUID aUuid, final BigInteger aNetworth)
    {
        /* Name: Captured by game menu */
        /* Networth: TBD*/
        /* UUID will be stored in a DB and we will grab their stored information from DB and assign it here */
        this.name = aName;
        this.id = aUuid;
        this.networth = aNetworth;
    }

    /* Turn logic */
    public abstract void turn();

    public void hit(final Card aCard ) {
        if(this.hand.canHit())
        {
            this.hand.addCard(aCard);
        }
    };

    public void loss()
    {
        this.networth = this.networth.subtract(hand.getBetTotal());
    }

    public void stand()             { this.hand.stand(); }
    public UUID getUuid()           { return this.id; }
    public String getName()         { return this.name; }
    public BigInteger getNetworth() { return this.networth; }
}
