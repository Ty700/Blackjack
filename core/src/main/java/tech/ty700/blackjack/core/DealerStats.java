package tech.ty700.blackjack.core;

import java.math.BigInteger;

/**
 * Lifetime dealer stats for the dealer leaderboard. House profit is the dealer's networth.
 */
public class DealerStats
{
    private long handsDealt = 0;
    private long blackjacks = 0;
    private long busts = 0;
    private BigInteger biggestPayoutLost = BigInteger.ZERO;
    private long playersBankrupted = 0;
    private long currentHouseWinStreak = 0;
    private long longestHouseWinStreak = 0;
    private long currentBustStreak = 0;
    private long longestBustStreak = 0;

    public long getHandsDealt()                 { return this.handsDealt; }
    public long getBlackjacks()                 { return this.blackjacks; }
    public long getBusts()                      { return this.busts; }
    public BigInteger getBiggestPayoutLost()    { return this.biggestPayoutLost; }
    public long getPlayersBankrupted()          { return this.playersBankrupted; }
    public long getCurrentHouseWinStreak()      { return this.currentHouseWinStreak; }
    public long getLongestHouseWinStreak()      { return this.longestHouseWinStreak; }
    public long getCurrentBustStreak()          { return this.currentBustStreak; }
    /* The "cursed dealer" stat */
    public long getLongestBustStreak()          { return this.longestBustStreak; }

    void recordPlayerBankrupted() { this.playersBankrupted++; }

    /* A player's winnings on one hand (payout minus their bet) */
    void recordPlayerWin(final BigInteger netWin)
    {
        if (netWin.compareTo(this.biggestPayoutLost) > 0) this.biggestPayoutLost = netWin;
    }

    /**
     * Called once per dealt round.
     * houseNet: chips the house gained (negative when it lost). A round the house didn't gain on breaks the streak.
     * dealerPlayed: false when the dealer never drew (blackjack, or every hand was already decided), which leaves the bust streak alone.
     */
    void recordRound(final BigInteger houseNet, final boolean blackjack, final boolean dealerPlayed, final boolean busted)
    {
        this.handsDealt++;
        if (blackjack) this.blackjacks++;

        if (houseNet.signum() > 0)
        {
            this.currentHouseWinStreak++;
            this.longestHouseWinStreak = Math.max(this.longestHouseWinStreak, this.currentHouseWinStreak);
        }
        else
        {
            this.currentHouseWinStreak = 0;
        }

        if (!dealerPlayed) return;
        if (busted)
        {
            this.busts++;
            this.currentBustStreak++;
            this.longestBustStreak = Math.max(this.longestBustStreak, this.currentBustStreak);
        }
        else
        {
            this.currentBustStreak = 0;
        }
    }
}
