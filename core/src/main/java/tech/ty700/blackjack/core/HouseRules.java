package tech.ty700.blackjack.core;

import java.math.BigInteger;

/**
 * Every tunable number in the game lives here, so balancing never means hunting through classes.
 */
public final class HouseRules
{
    private HouseRules() {}

    /* Betting */
    public static final BigInteger MIN_BET          = BigInteger.valueOf(100);
    public static final BigInteger BET_INCREMENT    = BigInteger.valueOf(25);
    public static final BigInteger STARTING_CHIPS   = BigInteger.valueOf(1_000);

    /* Table */
    public static final int MAX_HANDS                   = 4;
    public static final int BLACKJACK_MULTIPLIER        = 2;    /* 2:1, VIPs get more later */
    public static final int CARDS_PER_HAND_ESTIMATE     = 6;    /* Reshuffle estimate per hand in a round */
    public static final int DEALER_STANDS_ON            = 17;   /* Dealer hits soft 17 (H17) */

    /* Bankruptcy and loans */
    public static final BigInteger LOAN_AMOUNT          = BigInteger.valueOf(1_000);
    public static final BigInteger PAYOFF_ROUNDING      = BigInteger.valueOf(100);  /* The "hidden fee" */
    public static final int MAX_RISK                    = 10;
    public static final int RATE_STEP_TENTHS            = 25;   /* 2.5% per risk level, in tenths of a percent */
    public static final int MAX_RATE_TENTHS             = 250;  /* 25% */
    public static final int CLEAN_HANDS_PER_RISK_DROP   = 100;
    public static final int INTEREST_GRACE_HANDS        = 10;   /* First hands bet on a loan are interest-free */
    public static final int INTEREST_EVERY_HANDS        = 5;    /* After that, interest every 5th hand bet */
    public static final int BASE_REPAY_PERCENT          = 50;
    public static final int REPAY_STEP_PERCENT          = 10;
    public static final int MAX_REPAY_PERCENT           = 90;

    static
    {
        /* A min bet that isn't a multiple of the increment would make some valid-looking bets impossible */
        if (MIN_BET.mod(BET_INCREMENT).signum() != 0)
        {
            throw new IllegalStateException("MIN_BET must be a multiple of BET_INCREMENT");
        }
    }

    /* A positive multiple of BET_INCREMENT. Every chip amount that moves follows this */
    public static boolean isChipAmount(final BigInteger amount)
    {
        return amount.signum() > 0 && amount.mod(BET_INCREMENT).signum() == 0;
    }

    public static boolean isValidBet(final BigInteger amount)
    {
        return amount.compareTo(MIN_BET) >= 0 && isChipAmount(amount);
    }

    /* Interest rate for a risk level, in tenths of a percent (25 = 2.5%) */
    public static int rateTenthsFor(final int riskLevel)
    {
        return Math.min(RATE_STEP_TENTHS * riskLevel, MAX_RATE_TENTHS);
    }

    /* Share of a round's winnings that automatically goes to the loan */
    public static int repayPercentFor(final int riskLevel)
    {
        int percent = BASE_REPAY_PERCENT + REPAY_STEP_PERCENT * (riskLevel - 1);
        return Math.clamp(percent, BASE_REPAY_PERCENT, MAX_REPAY_PERCENT);
    }

    /* One hand of interest: debt * rate, rounded up to BET_INCREMENT. No floating point anywhere */
    static BigInteger interestFor(final BigInteger debt, final int rateTenths)
    {
        BigInteger thousand = BigInteger.valueOf(1_000);
        BigInteger interestTimesThousand = debt.multiply(BigInteger.valueOf(rateTenths));
        return roundUp(interestTimesThousand, BET_INCREMENT.multiply(thousand)).divide(thousand);
    }

    static BigInteger roundUp(final BigInteger amount, final BigInteger step)
    {
        BigInteger[] quotientAndRemainder = amount.divideAndRemainder(step);
        if (quotientAndRemainder[1].signum() == 0) return amount;
        return quotientAndRemainder[0].add(BigInteger.ONE).multiply(step);
    }

    static BigInteger roundDown(final BigInteger amount, final BigInteger step)
    {
        return amount.subtract(amount.mod(step));
    }
}
