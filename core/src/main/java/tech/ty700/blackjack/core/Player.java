package tech.ty700.blackjack.core;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

/**
 * Who the player is, their chips, their loan, and their lifetime stats.
 * Hands live in the Round, so splitting is handled there.
 */
public class Player extends Participant {

    private final PlayerStats stats = new PlayerStats();
    private int payoutMultiplier = HouseRules.BLACKJACK_MULTIPLIER;
    private int prestigeLevel = 0;

    /* Bankruptcy */
    private int riskLevel = 0;
    private int cleanHands = 0;
    private Loan loan = null;

    /* New player */
    public Player(final String name)
    {
        this(name, UUID.randomUUID(), HouseRules.STARTING_CHIPS);
    }

    /* Existing player. TODO: load stats, loan, and risk too once the DB exists */
    public Player(final String name, final UUID id, final BigInteger chips)
    {
        super(name, id, chips);
        this.stats.recordChips(chips);
    }

    public PlayerStats getStats()       { return this.stats; }
    /* Blackjack pays this many times the bet. VIP raises it */
    public int getPayoutMultiplier()    { return this.payoutMultiplier; }
    public int getPrestigeLevel()       { return this.prestigeLevel; }
    public int getRiskLevel()           { return this.riskLevel; }
    public Optional<Loan> getLoan()     { return Optional.ofNullable(this.loan); }
    public boolean isInDebt()           { return this.loan != null; }

    @Override
    protected void onChipsChanged()
    {
        this.stats.recordChips(this.getNetworth());
    }

    /* Bankruptcy is only for players who can't cover the minimum bet. Being in debt already doesn't stop it */
    public boolean canDeclareBankruptcy()
    {
        return !this.canAfford(HouseRules.MIN_BET);
    }

    void declareBankruptcy()
    {
        if (!canDeclareBankruptcy())
        {
            throw new IllegalStateException(this.getName() + " can't declare bankruptcy");
        }

        this.riskLevel = Math.min(this.riskLevel + 1, HouseRules.MAX_RISK);
        this.cleanHands = 0;
        int rate = HouseRules.rateTenthsFor(this.riskLevel);
        if (this.loan == null)
        {
            this.loan = new Loan(HouseRules.LOAN_AMOUNT, rate);
        }
        else
        {
            /* Still in debt: one loan, bigger debt, worse rate */
            this.loan.rollIn(HouseRules.LOAN_AMOUNT, rate);
        }
        this.stats.recordBankruptcy(this.loan.getRateTenths(), this.loan.getDebt());
        this.addChips(HouseRules.LOAN_AMOUNT);
    }

    /* Called once for every round the player bets in: interest compounds, clean hands count toward lower risk */
    void recordRoundBet()
    {
        if (this.loan != null)
        {
            this.loan.accrueInterest();
            this.stats.recordDebt(this.loan.getDebt(), this.loan.getHandsInDebt());
        }

        if (this.riskLevel > 0 && ++this.cleanHands >= HouseRules.CLEAN_HANDS_PER_RISK_DROP)
        {
            this.riskLevel--;
            this.cleanHands = 0;
        }
    }

    /* Takes the automatic share of a round's winnings. Returns what went to the house */
    BigInteger autoRepay(final BigInteger netWin)
    {
        if (this.loan == null || netWin.signum() <= 0) return BigInteger.ZERO;

        BigInteger percent = BigInteger.valueOf(HouseRules.repayPercentFor(this.riskLevel));
        BigInteger share = HouseRules.roundDown(netWin.multiply(percent).divide(BigInteger.valueOf(100)), HouseRules.BET_INCREMENT);
        if (share.signum() == 0) return BigInteger.ZERO;

        if (share.compareTo(this.loan.getDebt()) < 0)
        {
            payPartial(share);
            return share;
        }

        /* This share would clear the loan, so it becomes the payoff, fee included */
        BigInteger payoff = this.loan.getPayoffAmount();
        if (this.canAfford(payoff))
        {
            payOff();
            return payoff;
        }

        /* Can't cover the fee yet: leave one increment owing so the payoff (and its fee) still happens later */
        BigInteger partial = this.loan.getDebt().subtract(HouseRules.BET_INCREMENT);
        if (partial.signum() <= 0) return BigInteger.ZERO;
        payPartial(partial);
        return partial;
    }

    /* Early repayment. Paying the whole debt costs the payoff amount. Returns what went to the house */
    BigInteger repay(final BigInteger amount)
    {
        if (this.loan == null)
        {
            throw new IllegalStateException(this.getName() + " has no loan");
        }
        if (!HouseRules.isChipAmount(amount))
        {
            throw new IllegalArgumentException("Repayment must be a positive multiple of " + HouseRules.BET_INCREMENT);
        }

        if (amount.compareTo(this.loan.getDebt()) >= 0)
        {
            BigInteger payoff = this.loan.getPayoffAmount();
            if (!this.canAfford(payoff))
            {
                throw new IllegalStateException("Paying off the loan costs " + payoff);
            }
            payOff();
            return payoff;
        }

        payPartial(amount);
        return amount;
    }

    /* Reset to starting chips for a prestige level. Benefits come later */
    void prestige()
    {
        if (this.isInDebt())
        {
            throw new IllegalStateException("Pay off the loan before prestiging");
        }
        this.setChips(HouseRules.STARTING_CHIPS);
        this.prestigeLevel++;
    }

    private void payPartial(final BigInteger amount)
    {
        this.takeChips(amount);
        this.loan.pay(amount);
    }

    private void payOff()
    {
        this.takeChips(this.loan.getPayoffAmount());
        this.stats.recordLoanPaidOff(this.loan.payOff());
        this.loan = null;
    }
}
