package tech.ty700.blackjack.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigInteger;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static tech.ty700.blackjack.core.Rank.*;

/*
 * Spec (agreed design):
 *  - Bankruptcy: voluntary, only during betting, only below the 100 minimum bet.
 *    It raises risk by 1 (max 10) and lends a flat 1,000.
 *  - Going bankrupt while in debt adds the 1,000 to the existing debt and the whole loan moves to the new rate.
 *  - Rate = 2.5% x risk, max 25%, locked when the loan is taken.
 *  - Interest only counts rounds the player bets in. The first 10 are free, then interest compounds
 *    every 5th one (hands 15, 20, 25...), each charge rounded up to 25. Rolling in a loan doesn't restart the free hands.
 *    A 1,000 loan after 10 charges: risk 1 = 1,475 ... risk 10 = 9,500.
 *  - Auto repayment takes 50% + 10% x (risk - 1) of a round's winnings (max 90%, never below 50%), rounded down to 25.
 *  - Paying off the loan rounds the final payment up to the next 100 (hidden fee). Early payment is allowed.
 *  - Repayments go to the house ledger, not the dealer.
 *  - Every 100 rounds bet without going bankrupt lowers risk by 1 (min 0).
 *  - No prestige while in debt. Prestige resets chips to 1,000 and raises the prestige level.
 */
public class EconomyTest
{
    private Dealer dealer;
    private Player broke;

    @BeforeEach
    void setUp()
    {
        dealer = new Dealer("House");
        broke = new Player("Broke", UUID.randomUUID(), chips(75));
    }

    private static BigInteger chips(long n) { return BigInteger.valueOf(n); }

    private Table table(Deck deck, Player p)
    {
        Table t = new Table(dealer, deck);
        t.playerConnect(p, 0);
        return t;
    }

    private static Loan loanOf(Player p)
    {
        return p.getLoan().orElseThrow();
    }

    /* ---------- Formulas ---------- */

    @ParameterizedTest(name = "risk {0} -> {1} tenths")
    @CsvSource({ "0, 0", "1, 25", "2, 50", "4, 100", "6, 150", "8, 200", "10, 250", "11, 250" })
    void rateIsTwoPointFivePercentPerRiskCappedAt25(int risk, int tenths)
    {
        assertEquals(tenths, HouseRules.rateTenthsFor(risk));
    }

    @ParameterizedTest(name = "risk {0} -> {1}%")
    @CsvSource({ "0, 50", "1, 50", "2, 60", "3, 70", "4, 80", "5, 90", "10, 90" })
    void repayShareRisesTenPercentPerRiskCappedAt90(int risk, int percent)
    {
        assertEquals(percent, HouseRules.repayPercentFor(risk));
    }

    /* Hands it takes to get n interest charges */
    private static int handsForCharges(int n)
    {
        return HouseRules.INTEREST_GRACE_HANDS + n * HouseRules.INTEREST_EVERY_HANDS;
    }

    @ParameterizedTest(name = "risk {0} -> {1} after 10 charges")
    @CsvSource({
            "1, 1475", "2, 1775", "3, 2225", "4, 2725", "5, 3450",
            "6, 4250", "7, 5275", "8, 6400", "9, 7925", "10, 9500",
    })
    void debtAfterTenChargesMatchesTheTable(int risk, long expected)
    {
        Loan loan = new Loan(chips(1_000), HouseRules.rateTenthsFor(risk));
        for (int i = 0; i < handsForCharges(10); i++)
        {
            loan.accrueInterest();
        }
        assertEquals(chips(expected), loan.getDebt());
    }

    @Test
    void firstTenHandsAreInterestFree()
    {
        Loan loan = new Loan(chips(1_000), 25);
        for (int i = 0; i < 10; i++) loan.accrueInterest();
        assertEquals(chips(1000), loan.getDebt());
    }

    @Test
    void interestComesEveryFifthHandAfterTheGracePeriod()
    {
        Loan loan = new Loan(chips(1_000), 25);
        for (int i = 0; i < 14; i++) loan.accrueInterest();
        assertEquals(chips(1000), loan.getDebt());
        assertEquals(1, loan.getHandsUntilInterest());

        loan.accrueInterest();
        assertEquals(chips(1025), loan.getDebt());
        assertEquals(5, loan.getHandsUntilInterest());

        for (int i = 0; i < 4; i++) loan.accrueInterest();
        assertEquals(chips(1025), loan.getDebt());
        /* 2.5% of 1,025 is 25.6, rounded up to 50 */
        loan.accrueInterest();
        assertEquals(chips(1075), loan.getDebt());
    }

    /* ---------- Declaring bankruptcy ---------- */

    @Test
    void bankruptcyLendsAFlatThousandAtRiskOne()
    {
        Round r = table(new StackedDeck(), broke).startRound();
        r.declareBankruptcy(broke);

        assertEquals(chips(1075), broke.getNetworth());
        assertEquals(1, broke.getRiskLevel());
        assertEquals(chips(1000), loanOf(broke).getDebt());
        assertEquals(25, loanOf(broke).getRateTenths());
        assertEquals(1, broke.getStats().getTimesBankrupt());
        assertEquals(1, dealer.getStats().getPlayersBankrupted());
    }

    @Test
    void cannotDeclareBankruptcyWithEnoughForTheMinimumBet()
    {
        Player fine = new Player("Fine", UUID.randomUUID(), chips(100));
        Round r = table(new StackedDeck(), fine).startRound();
        assertThrows(IllegalStateException.class, () -> r.declareBankruptcy(fine));
    }

    @Test
    void bankruptcyWhileInDebtRollsIntoTheSameLoanAtTheNewRate()
    {
        Round r = table(new StackedDeck(), broke).startRound();
        r.declareBankruptcy(broke);
        broke.takeChips(chips(1_025));

        r.declareBankruptcy(broke);

        assertEquals(2, broke.getRiskLevel());
        assertEquals(chips(2000), loanOf(broke).getDebt());
        assertEquals(50, loanOf(broke).getRateTenths());
        assertEquals(chips(1050), broke.getNetworth());
        assertEquals(2, broke.getStats().getTimesBankrupt());
        assertEquals(chips(2000), broke.getStats().getPeakDebt());
    }

    @Test
    void rollingInALoanDoesNotRestartTheFreeHands()
    {
        broke.declareBankruptcy();
        for (int i = 0; i < 9; i++) broke.recordRoundBet();
        broke.setChips(BigInteger.ZERO);
        broke.declareBankruptcy();

        assertEquals(9, loanOf(broke).getHandsInDebt());
        assertEquals(6, loanOf(broke).getHandsUntilInterest());
    }

    @Test
    void rolledLoanCountsAllExtraPaidAsInterest()
    {
        broke.declareBankruptcy();
        broke.setChips(BigInteger.ZERO);
        broke.declareBankruptcy();
        for (int i = 0; i < handsForCharges(1); i++) broke.recordRoundBet();
        /* 2,000 at 5% = 2,100, payoff 2,100 */
        broke.addChips(chips(1_100));

        assertEquals(chips(2100), broke.repay(chips(2100)));
        assertEquals(chips(100), broke.getStats().getMostInterestOnOneLoan());
    }

    @Test
    void cannotDeclareBankruptcyOutsideBetting()
    {
        Player alice = new Player("Alice");
        Table t = new Table(dealer, new StackedDeck(TEN, TEN, NINE, SEVEN));
        t.playerConnect(alice, 0);
        t.playerConnect(broke, 1);
        Round r = t.startRound();
        r.placeBet(alice, chips(100));
        r.closeBetting();

        assertThrows(IllegalStateException.class, () -> r.declareBankruptcy(broke));
    }

    /* ---------- Interest ---------- */

    @Test
    void interestOnlyGrowsOnRoundsThePlayerBetsIn()
    {
        Table t = table(new StackedDeck(TEN, TEN, SEVEN, SEVEN), broke);
        Round first = t.startRound();
        first.declareBankruptcy(broke);
        first.closeBetting();
        assertEquals(0, loanOf(broke).getHandsInDebt());

        /* Push, so nothing but the hand count changes */
        Round second = t.startRound();
        second.placeBet(broke, chips(100));
        second.closeBetting();
        second.apply(broke, Action.STAND);

        assertEquals(1, loanOf(broke).getHandsInDebt());
        assertEquals(chips(1075), broke.getNetworth());
    }

    @Test
    void rateIsLockedWhenTheLoanIsTaken()
    {
        broke.declareBankruptcy();
        for (int i = 0; i < HouseRules.CLEAN_HANDS_PER_RISK_DROP; i++)
        {
            broke.recordRoundBet();
        }

        assertEquals(0, broke.getRiskLevel());
        assertEquals(25, loanOf(broke).getRateTenths());
    }

    /* ---------- Repayment ---------- */

    @Test
    void winningsAutomaticallyRepayHalfAtRiskOne()
    {
        Table t = table(new StackedDeck(TEN, TEN, NINE, SEVEN), broke);
        Round r = t.startRound();
        r.declareBankruptcy(broke);
        r.placeBet(broke, chips(100));
        r.closeBetting();
        r.apply(broke, Action.STAND);

        /* Still in the free hands. Won 100, half (50) goes to the loan */
        assertEquals(chips(950), loanOf(broke).getDebt());
        assertEquals(chips(1125), broke.getNetworth());
        assertEquals(chips(50), t.drainHouseRepayments());
        assertEquals(BigInteger.ZERO, t.drainHouseRepayments());
    }

    @Test
    void automaticShareRoundsDownTo25()
    {
        broke.declareBankruptcy();
        /* Half of 125 is 62.5, rounded down to 50 */
        assertEquals(chips(50), broke.autoRepay(chips(125)));
        assertEquals(chips(950), loanOf(broke).getDebt());
    }

    @Test
    void payingOffRoundsUpToTheNext100()
    {
        Table t = table(new StackedDeck(), broke);
        broke.declareBankruptcy();
        for (int i = 0; i < handsForCharges(1); i++) broke.recordRoundBet();
        /* Debt 1,025, payoff 1,100, and 1,075 chips */
        broke.addChips(chips(25));

        BigInteger paid = t.repayLoan(broke, chips(1025));

        assertEquals(chips(1100), paid);
        assertFalse(broke.isInDebt());
        assertEquals(BigInteger.ZERO, broke.getNetworth());
        assertEquals(chips(100), broke.getStats().getTotalInterestPaid());
        assertEquals(chips(100), broke.getStats().getMostInterestOnOneLoan());
        assertEquals(chips(1100), t.drainHouseRepayments());
    }

    @Test
    void earlyPartialRepaymentLowersTheDebt()
    {
        Table t = table(new StackedDeck(), broke);
        broke.declareBankruptcy();

        t.repayLoan(broke, chips(100));
        assertEquals(chips(900), loanOf(broke).getDebt());
        assertThrows(IllegalArgumentException.class, () -> t.repayLoan(broke, chips(30)));
    }

    /* ---------- Risk ---------- */

    @Test
    void riskStopsAtTen()
    {
        for (int i = 0; i < 12; i++)
        {
            broke.setChips(BigInteger.ZERO);
            broke.declareBankruptcy();
            broke.addChips(chips(1_000_000));
            broke.repay(loanOf(broke).getDebt());
        }

        assertEquals(10, broke.getRiskLevel());
        assertEquals(250, broke.getStats().getHighestRateTenths());
        assertEquals(12, broke.getStats().getTimesBankrupt());
    }

    @Test
    void hundredCleanRoundsLowerRiskByOne()
    {
        broke.declareBankruptcy();
        for (int i = 0; i < HouseRules.CLEAN_HANDS_PER_RISK_DROP - 1; i++)
        {
            broke.recordRoundBet();
        }
        assertEquals(1, broke.getRiskLevel());

        broke.recordRoundBet();
        assertEquals(0, broke.getRiskLevel());
    }

    @Test
    void debtStatsTrackPeakAndTimeInDebt()
    {
        broke.declareBankruptcy();
        for (int i = 0; i < handsForCharges(2); i++) broke.recordRoundBet();

        assertEquals(chips(1075), broke.getStats().getPeakDebt());
        assertEquals(handsForCharges(2), broke.getStats().getLongestTimeInDebt());
    }

    /* ---------- Prestige ---------- */

    @Test
    void cannotPrestigeWhileInDebt()
    {
        Table t = table(new StackedDeck(), broke);
        broke.declareBankruptcy();
        assertThrows(IllegalStateException.class, () -> t.prestige(broke));
    }

    @Test
    void prestigeResetsChipsAndRaisesTheLevel()
    {
        Player rich = new Player("Rich", UUID.randomUUID(), chips(5_000_000));
        Table t = table(new StackedDeck(), rich);
        t.prestige(rich);

        assertEquals(chips(1000), rich.getNetworth());
        assertEquals(1, rich.getPrestigeLevel());
    }
}
