package tech.ty700.blackjack.cli;

import tech.ty700.blackjack.core.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Terminal blackjack for one player: Tyler, in the middle seat with the house.
 * The table redraws in place every time. Bots can fill the other seats later.
 * No DB yet, so chips reset every time the program starts.
 */
public class Main {
    /* Seats are 0 to 6, so 3 is the middle */
    private static final int TYLER_SEAT = 3;

    private final Player tyler = new Player("Tyler");
    private final Table table = new Table(new Dealer("House"));
    private final TableScreen screen = new TableScreen();

    /* Shown under the table on the next draw, then cleared */
    private final List<String> messages = new ArrayList<>();
    /* The last finished round stays on the table while the next bet is placed */
    private RoundView lastRound = null;
    /* What the current round looks like on screen, so new cards can be dealt in one at a time */
    private RoundView onScreen = null;

    /* Pause between cards, and a longer one when the dealer flips the hole card */
    private static final long CARD_DELAY_MS = 450;
    private static final long FLIP_DELAY_MS = 700;

    void main()
    {
        this.table.playerConnect(this.tyler, TYLER_SEAT);
        this.messages.add("Blackjack pays " + this.tyler.getPayoutMultiplier() + ":1. Dealer hits soft 17. Type 'help' for commands.");

        while (true)
        {
            Round round = this.table.startRound();
            if (!betting(round)) break;

            /* Read after betting, so a loan or repayment made at the prompt isn't counted as winnings */
            BigInteger chipsBefore = this.tyler.getNetworth().add(betOf(round));
            BigInteger loanPaidBefore = loanPaid();
            boolean inDebtBefore = this.tyler.isInDebt();

            round.closeBetting();
            this.onScreen = null;
            if (!round.hasBet(this.tyler))
            {
                this.messages.add("You sat that one out.");
                continue;
            }

            while (round.getPhase() == Round.Phase.PLAYER_TURNS)
            {
                if (!playTurn(round)) return;
            }

            this.lastRound = round.getView();
            show(this.lastRound);
            pause(FLIP_DELAY_MS);
            addResults(chipsBefore, loanPaidBefore, inDebtBefore);
        }

        this.messages.clear();
        addStats();
        this.messages.add("Thanks for playing, Tyler.");
        draw(this.lastRound);
    }

    /* ---------- Betting ---------- */

    /* Returns false when Tyler quits */
    private boolean betting(final Round round)
    {
        while (true)
        {
            if (this.tyler.canDeclareBankruptcy())
            {
                this.messages.add("You can't cover the minimum bet. Type 'bankrupt' to take a house loan"
                        + (this.tyler.isInDebt() ? " (it adds to your debt at a higher rate)." : "."));
            }

            draw(this.lastRound != null ? this.lastRound : round.getView());
            String input = IO.readln("Bet (min " + chips(HouseRules.MIN_BET) + ", steps of "
                    + chips(HouseRules.BET_INCREMENT) + ") > ");
            if (input == null) return false;
            this.messages.clear();

            String[] parts = input.trim().toLowerCase().split("\\s+");
            try
            {
                switch (parts[0])
                {
                    case "q", "quit", "exit" -> { return false; }
                    case "", "sit" -> { return true; }
                    case "help" -> addHelp();
                    case "stats" -> addStats();
                    case "bankrupt" ->
                    {
                        round.declareBankruptcy(this.tyler);
                        Loan loan = this.tyler.getLoan().orElseThrow();
                        this.messages.add("Bankrupt. The house lends you " + chips(HouseRules.LOAN_AMOUNT)
                                + ". You owe " + chips(loan.getDebt()) + " at " + rate(loan.getRateTenths())
                                + " per hand. Risk is now " + this.tyler.getRiskLevel() + ".");
                    }
                    case "repay" ->
                    {
                        if (parts.length < 2) throw new IllegalArgumentException("Usage: repay <amount>");
                        BigInteger paid = this.table.repayLoan(this.tyler, parseAmount(parts[1]));
                        this.messages.add("Paid " + chips(paid) + " toward the loan.");
                    }
                    case "payoff" ->
                    {
                        Loan loan = this.tyler.getLoan().orElseThrow(() -> new IllegalStateException("You have no loan"));
                        BigInteger paid = this.table.repayLoan(this.tyler, loan.getDebt());
                        this.messages.add("Loan paid off for " + chips(paid) + ".");
                    }
                    case "prestige" ->
                    {
                        String answer = IO.readln("Prestige resets you to " + chips(HouseRules.STARTING_CHIPS) + " chips. Sure? (y/n) > ");
                        if (answer == null || !answer.trim().toLowerCase().startsWith("y")) break;
                        this.table.prestige(this.tyler);
                        this.messages.add("Prestige " + this.tyler.getPrestigeLevel() + "!");
                    }
                    case "all", "max" ->
                    {
                        BigInteger all = this.tyler.getNetworth().subtract(this.tyler.getNetworth().mod(HouseRules.BET_INCREMENT));
                        round.placeBet(this.tyler, all);
                        return true;
                    }
                    default ->
                    {
                        round.placeBet(this.tyler, parseAmount(parts[0]));
                        return true;
                    }
                }
            }
            catch (IllegalArgumentException | IllegalStateException e)
            {
                this.messages.add(e.getMessage());
            }
        }
    }

    /* ---------- Playing ---------- */

    /* Returns false when Tyler quits mid-hand */
    private boolean playTurn(final Round round)
    {
        Set<Action> legal = round.getLegalActions(this.tyler);
        StringBuilder prompt = new StringBuilder();
        for (Action a : List.of(Action.HIT, Action.STAND, Action.DOUBLE, Action.SPLIT))
        {
            if (legal.contains(a)) prompt.append(label(a)).append("  ");
        }
        prompt.append("> ");

        show(round.getView());
        while (true)
        {
            String input = IO.readln(prompt.toString());
            if (input == null) return false;
            this.messages.clear();

            Action action = switch (input.trim().toLowerCase())
            {
                case "h", "hit" -> Action.HIT;
                case "s", "stand" -> Action.STAND;
                case "d", "double" -> Action.DOUBLE;
                case "p", "split" -> Action.SPLIT;
                default -> null;
            };

            if (action == null || !legal.contains(action))
            {
                this.messages.add("Pick one of: " + prompt.toString().replace(">", "").trim());
                draw(round.getView());
                continue;
            }

            round.apply(this.tyler, action);
            return true;
        }
    }

    /* ---------- Messages ---------- */

    /* Deals any new cards in one at a time, then draws the full view */
    private void show(final RoundView next)
    {
        for (DealAnimation.Frame frame : DealAnimation.frames(this.onScreen, next))
        {
            draw(frame.view());
            pause(frame.flip() ? FLIP_DELAY_MS : CARD_DELAY_MS);
        }
        draw(next);
        this.onScreen = next;
    }

    private static void pause(final long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private void draw(final RoundView view)
    {
        this.screen.draw(view, this.table.getSeatedPlayers(), this.tyler, status(), this.messages);
    }

    private void addResults(final BigInteger chipsBefore, final BigInteger loanPaidBefore, final boolean inDebtBefore)
    {
        BigInteger change = this.tyler.getNetworth().subtract(chipsBefore);
        if (change.signum() > 0) this.messages.add("You won " + chips(change) + ".");
        else if (change.signum() < 0) this.messages.add("You lost " + chips(change.negate()) + ".");
        else this.messages.add("Even.");

        if (inDebtBefore && !this.tyler.isInDebt())
        {
            this.messages.add("Your winnings paid off the loan!");
            return;
        }

        BigInteger repaid = loanPaid().subtract(loanPaidBefore);
        if (repaid.signum() > 0)
        {
            this.messages.add(chips(repaid) + " of your winnings went to the loan.");
        }
    }

    private void addStats()
    {
        PlayerStats s = this.tyler.getStats();
        this.messages.add("--- Tyler's stats ---");
        this.messages.add("Hands: " + s.getHandsPlayed() + " (won " + s.getHandsWon() + ", lost " + s.getHandsLost()
                + ", pushed " + s.getHandsPushed() + ")");
        this.messages.add("Blackjacks: " + s.getBlackjacks() + "   Busts: " + s.getBusts()
                + "   5-card Charlies: " + s.getFiveCardCharlies());
        this.messages.add("Doubles: " + s.getDoublesTaken() + " (won " + s.getDoublesWon() + ")   Splits: " + s.getSplits());
        this.messages.add("Win streak: " + s.getCurrentWinStreak() + " (best " + s.getLongestWinStreak() + ")");
        this.messages.add("Biggest win: " + chips(s.getBiggestWin()) + "   Biggest bet: " + chips(s.getBiggestBet()));
        this.messages.add("Total wagered: " + chips(s.getTotalWagered()) + "   Net profit: " + chips(s.getNetProfit()));
        this.messages.add("Peak chips: " + chips(s.getPeakChips()) + "   Prestige: " + this.tyler.getPrestigeLevel());
        if (s.getTimesBankrupt() > 0)
        {
            this.messages.add("Bankruptcies: " + s.getTimesBankrupt() + "   Interest paid: " + chips(s.getTotalInterestPaid())
                    + "   Peak debt: " + chips(s.getPeakDebt()) + "   Highest rate: " + rate(s.getHighestRateTenths()));
        }
    }

    private void addHelp()
    {
        this.messages.add("<amount>        bet that many chips (e.g. 250 or 1,000)");
        this.messages.add("all             bet everything you can");
        this.messages.add("sit             sit out this round (or just press Enter)");
        this.messages.add("bankrupt        take a house loan (only below the minimum bet)");
        this.messages.add("repay <amount>  pay down your loan early");
        this.messages.add("payoff          pay off the whole loan (rounded up to the next 100)");
        this.messages.add("prestige        reset to starting chips for a prestige level (no debt allowed)");
        this.messages.add("stats           show your stats");
        this.messages.add("quit            leave the table");
        this.messages.add("In a hand: h = hit, s = stand, d = double, p = split");
    }

    private String status()
    {
        StringBuilder sb = new StringBuilder("Chips: " + chips(this.tyler.getNetworth()));
        this.tyler.getLoan().ifPresent(loan -> sb.append("   Debt: ").append(chips(loan.getDebt()))
                .append(" at ").append(rate(loan.getRateTenths()))
                .append(" (payoff ").append(chips(loan.getPayoffAmount())).append(")")
                .append("   Interest in ").append(loan.getHandsUntilInterest())
                .append(loan.getHandsUntilInterest() == 1 ? " hand" : " hands"));
        if (this.tyler.getRiskLevel() > 0) sb.append("   Risk: ").append(this.tyler.getRiskLevel());
        if (this.tyler.getPrestigeLevel() > 0) sb.append("   Prestige: ").append(this.tyler.getPrestigeLevel());
        return sb.toString();
    }

    /* ---------- Helpers ---------- */

    private BigInteger betOf(final Round round)
    {
        return round.getView().seats().stream()
                .filter(seat -> seat.playerId().equals(this.tyler.getUuid()) && seat.bet() != null)
                .map(RoundView.SeatView::bet)
                .findFirst()
                .orElse(BigInteger.ZERO);
    }

    private BigInteger loanPaid()
    {
        return this.tyler.getLoan().map(Loan::getTotalPaid).orElse(BigInteger.ZERO);
    }

    private static String label(final Action a)
    {
        return switch (a)
        {
            case HIT -> "[h]it";
            case STAND -> "[s]tand";
            case DOUBLE -> "[d]ouble";
            case SPLIT -> "s[p]lit";
        };
    }

    private static String chips(final BigInteger amount)
    {
        return String.format("%,d", amount);
    }

    private static String rate(final int tenths)
    {
        return (tenths / 10) + (tenths % 10 == 0 ? "" : "." + (tenths % 10)) + "%";
    }

    private static BigInteger parseAmount(final String text)
    {
        try
        {
            return new BigInteger(text.replace(",", "").replace("_", ""));
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Not a command or an amount: " + text);
        }
    }
}
