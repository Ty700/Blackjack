package tech.ty700.blackjack.core;

import java.math.BigInteger;
import java.util.UUID;

/**
 * Dealers come from a pool (the DB, later) and are handed to a Table.
 * Networth is the house profit at this dealer's tables, and it can go negative.
 */
public class Dealer extends Participant
{
    private final DealerStats stats = new DealerStats();

    /* New dealer for the pool */
    public Dealer(final String aName)
    {
        this(aName, UUID.randomUUID(), BigInteger.ZERO);
    }

    /* Dealer loaded from the pool */
    public Dealer(final String aName, final UUID aUuid, final BigInteger aHouseProfit)
    {
        super(aName, aUuid, aHouseProfit);
    }

    public DealerStats getStats()       { return this.stats; }
    public BigInteger getHouseProfit()  { return this.getNetworth(); }

    /* H17: hit below 17, and hit a soft 17 */
    public boolean shouldHit(final Hand h)
    {
        int total = h.getHandTotal();
        return total < HouseRules.DEALER_STANDS_ON || (total == HouseRules.DEALER_STANDS_ON && h.isSoft());
    }
}
