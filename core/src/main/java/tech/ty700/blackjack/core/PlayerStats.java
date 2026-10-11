package tech.ty700.blackjack.core;

import java.math.BigInteger;

/**
 * Lifetime player stats for the leaderboards. Only core records them.
 */
public class PlayerStats
{
    private long handsPlayed = 0;
    private long handsWon = 0;
    private long handsLost = 0;
    private long handsPushed = 0;
    private long blackjacks = 0;
    private long busts = 0;
    private BigInteger biggestWin = BigInteger.ZERO;
    private BigInteger biggestBet = BigInteger.ZERO;
    private BigInteger totalWagered = BigInteger.ZERO;
    private BigInteger netProfit = BigInteger.ZERO;
    private BigInteger peakChips = BigInteger.ZERO;
    private long currentWinStreak = 0;
    private long longestWinStreak = 0;
    private long doublesTaken = 0;
    private long doublesWon = 0;
    private long splits = 0;
    private long fiveCardCharlies = 0;

    /* Bankruptcy */
    private long timesBankrupt = 0;
    private long loansTaken = 0;
    private BigInteger totalInterestPaid = BigInteger.ZERO;
    private BigInteger peakDebt = BigInteger.ZERO;
    private BigInteger mostInterestOnOneLoan = BigInteger.ZERO;
    private long longestTimeInDebt = 0;
    private int highestRateTenths = 0;

    public long getHandsPlayed()                    { return this.handsPlayed; }
    public long getHandsWon()                       { return this.handsWon; }
    public long getHandsLost()                      { return this.handsLost; }
    public long getHandsPushed()                    { return this.handsPushed; }
    public long getBlackjacks()                     { return this.blackjacks; }
    public long getBusts()                          { return this.busts; }
    public BigInteger getBiggestWin()               { return this.biggestWin; }
    public BigInteger getBiggestBet()               { return this.biggestBet; }
    public BigInteger getTotalWagered()             { return this.totalWagered; }
    public BigInteger getNetProfit()                { return this.netProfit; }
    public BigInteger getPeakChips()                { return this.peakChips; }
    public long getCurrentWinStreak()               { return this.currentWinStreak; }
    public long getLongestWinStreak()               { return this.longestWinStreak; }
    public long getDoublesTaken()                   { return this.doublesTaken; }
    public long getDoublesWon()                     { return this.doublesWon; }
    public long getSplits()                         { return this.splits; }
    public long getFiveCardCharlies()               { return this.fiveCardCharlies; }
    public long getTimesBankrupt()                  { return this.timesBankrupt; }
    public long getLoansTaken()                     { return this.loansTaken; }
    public BigInteger getTotalInterestPaid()        { return this.totalInterestPaid; }
    public BigInteger getPeakDebt()                 { return this.peakDebt; }
    public BigInteger getMostInterestOnOneLoan()    { return this.mostInterestOnOneLoan; }
    public long getLongestTimeInDebt()              { return this.longestTimeInDebt; }
    /* Tenths of a percent: 25 = 2.5% */
    public int getHighestRateTenths()               { return this.highestRateTenths; }

    void recordChips(final BigInteger chips)
    {
        this.peakChips = this.peakChips.max(chips);
    }

    /* The opening bet of a round */
    void recordOpeningBet(final BigInteger bet)
    {
        this.biggestBet = this.biggestBet.max(bet);
    }

    /* Every chip put on the table: opening bets, doubles, splits */
    void recordWager(final BigInteger amount)
    {
        this.totalWagered = this.totalWagered.add(amount);
    }

    void recordSplit() { this.splits++; }

    /* One settled hand. payout is everything handed back, including the returned bet */
    void recordHand(final Hand hand, final Hand.HandResult outcome, final BigInteger payout, final boolean doubled)
    {
        this.handsPlayed++;
        BigInteger net = payout.subtract(hand.getBetTotal());
        this.netProfit = this.netProfit.add(net);
        this.biggestWin = this.biggestWin.max(net);

        if (doubled) this.doublesTaken++;
        /* Counts even when a dealer blackjack turns it into a push */
        if (hand.getHand().size() == 2 && hand.getHandTotal() == 21) this.blackjacks++;
        if (hand.getHand().size() >= 5 && outcome != Hand.HandResult.BUSTED) this.fiveCardCharlies++;

        switch (outcome)
        {
            case WON, BLACKJACK ->
            {
                this.handsWon++;
                if (doubled) this.doublesWon++;
                this.currentWinStreak++;
                this.longestWinStreak = Math.max(this.longestWinStreak, this.currentWinStreak);
            }
            case LOST, BUSTED ->
            {
                this.handsLost++;
                if (outcome == Hand.HandResult.BUSTED) this.busts++;
                this.currentWinStreak = 0;
            }
            /* A push leaves the streak alone */
            case PUSHED -> this.handsPushed++;
            case IN_PROGRESS -> throw new IllegalArgumentException("Can't record an unsettled hand");
        }
    }

    void recordBankruptcy(final int rateTenths, final BigInteger debt)
    {
        this.timesBankrupt++;
        this.loansTaken++;
        this.highestRateTenths = Math.max(this.highestRateTenths, rateTenths);
        this.peakDebt = this.peakDebt.max(debt);
    }

    void recordDebt(final BigInteger debt, final long handsInDebt)
    {
        this.peakDebt = this.peakDebt.max(debt);
        this.longestTimeInDebt = Math.max(this.longestTimeInDebt, handsInDebt);
    }

    void recordLoanPaidOff(final BigInteger interestPaid)
    {
        this.totalInterestPaid = this.totalInterestPaid.add(interestPaid);
        this.mostInterestOnOneLoan = this.mostInterestOnOneLoan.max(interestPaid);
    }
}
