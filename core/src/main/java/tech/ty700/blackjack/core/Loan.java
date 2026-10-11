package tech.ty700.blackjack.core;

import java.math.BigInteger;

/**
 * A house loan from bankruptcy. The rate is locked when the loan is taken.
 * Going bankrupt again while in debt rolls the new loan into this one at the new (higher) rate.
 * Debt stays a multiple of BET_INCREMENT: interest rounds up, repayments round down.
 */
public class Loan
{
    private BigInteger principal;
    private int rateTenths;
    private BigInteger debt;
    private BigInteger totalPaid = BigInteger.ZERO;
    private long handsInDebt = 0;

    Loan(final BigInteger aPrincipal, final int aRateTenths)
    {
        this.principal = aPrincipal;
        this.rateTenths = aRateTenths;
        this.debt = aPrincipal;
    }

    public BigInteger getPrincipal()    { return this.principal; }
    /* Tenths of a percent: 25 = 2.5% */
    public int getRateTenths()          { return this.rateTenths; }
    public BigInteger getDebt()         { return this.debt; }
    public BigInteger getTotalPaid()    { return this.totalPaid; }
    public long getHandsInDebt()        { return this.handsInDebt; }

    /* What clearing the loan costs: the debt rounded up to the next 100 (the hidden fee) */
    public BigInteger getPayoffAmount()
    {
        return HouseRules.roundUp(this.debt, HouseRules.PAYOFF_ROUNDING);
    }

    /* Another bankruptcy: add the new loan to the debt and lock the whole thing at the new rate */
    void rollIn(final BigInteger additional, final int newRateTenths)
    {
        this.principal = this.principal.add(additional);
        this.debt = this.debt.add(additional);
        this.rateTenths = newRateTenths;
    }

    /* Hands bet until the next interest charge (1 means the next hand) */
    public long getHandsUntilInterest()
    {
        long grace = HouseRules.INTEREST_GRACE_HANDS;
        long every = HouseRules.INTEREST_EVERY_HANDS;
        if (this.handsInDebt < grace) return grace + every - this.handsInDebt;
        return every - (this.handsInDebt - grace) % every;
    }

    /* One hand bet with the loan open. Free during the grace period, then every INTEREST_EVERY_HANDS hands */
    BigInteger accrueInterest()
    {
        this.handsInDebt++;
        long pastGrace = this.handsInDebt - HouseRules.INTEREST_GRACE_HANDS;
        if (pastGrace <= 0 || pastGrace % HouseRules.INTEREST_EVERY_HANDS != 0) return BigInteger.ZERO;

        BigInteger interest = HouseRules.interestFor(this.debt, this.rateTenths);
        this.debt = this.debt.add(interest);
        return interest;
    }

    /* Partial payment, always less than the debt */
    void pay(final BigInteger amount)
    {
        if (amount.compareTo(this.debt) >= 0)
        {
            throw new IllegalArgumentException("A payment that clears the debt must go through payOff");
        }
        this.debt = this.debt.subtract(amount);
        this.totalPaid = this.totalPaid.add(amount);
    }

    /* Clears the loan. Returns what was paid on top of the principal (interest plus fee) */
    BigInteger payOff()
    {
        this.totalPaid = this.totalPaid.add(getPayoffAmount());
        this.debt = BigInteger.ZERO;
        return this.totalPaid.subtract(this.principal);
    }
}
