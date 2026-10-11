package tech.ty700.blackjack.core;

import java.math.BigInteger;

/**
 * Loan repayments waiting to be added to the global house total.
 * core never holds the global total: the server drains this and adds it to the DB atomically.
 */
class HouseLedger
{
    private BigInteger pending = BigInteger.ZERO;

    void add(final BigInteger amount)
    {
        this.pending = this.pending.add(amount);
    }

    BigInteger drain()
    {
        BigInteger drained = this.pending;
        this.pending = BigInteger.ZERO;
        return drained;
    }
}
