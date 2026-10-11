package tech.ty700.blackjack.cli;

import tech.ty700.blackjack.core.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI players for the other seats. They bet a random amount, play basic strategy,
 * and leave the table when they can't cover the minimum bet.
 */
class Bots
{
    private static final List<String> NAMES = List.of(
            "Rusty", "Lola", "Dex", "Mabel", "Vinny", "Opal", "Gus", "Trixie", "Sal", "Penny", "Moe", "Ruby");

    /* Chance a bot sits a round out */
    private static final double SIT_OUT_CHANCE = 0.1;
    /* Bets are at most this share of a bot's chips (but never below the minimum) */
    private static final int MAX_BET_PERCENT = 20;

    private final List<Player> bots = new ArrayList<>();

    /* Seats a random number of bots (at least one) in random open seats */
    void fill(final Table table)
    {
        List<String> names = new ArrayList<>(NAMES);
        Collections.shuffle(names);

        int open = Table.MAX_SEATS - table.getSeatedPlayers().size();
        int count = ThreadLocalRandom.current().nextInt(1, open + 1);
        for (int i = 0; i < count; i++)
        {
            Player bot = new Player(names.get(i));
            table.playerConnect(bot);
            this.bots.add(bot);
        }
    }

    boolean isBot(final Player p)
    {
        return this.bots.stream().anyMatch(b -> b.getUuid().equals(p.getUuid()));
    }

    /* Broke bots get up from the table. Returns who left */
    List<Player> removeBroke(final Table table)
    {
        List<Player> left = new ArrayList<>();
        for (Player bot : List.copyOf(this.bots))
        {
            if (!bot.canAfford(HouseRules.MIN_BET))
            {
                table.playerDisconnect(bot);
                this.bots.remove(bot);
                left.add(bot);
            }
        }
        return left;
    }

    void placeBets(final Round round)
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Player bot : this.bots)
        {
            if (!round.isInRound(bot) || random.nextDouble() < SIT_OUT_CHANCE) continue;

            /* A random multiple of the bet step between the minimum and MAX_BET_PERCENT of their chips */
            BigInteger step = HouseRules.BET_INCREMENT;
            BigInteger cap = bot.getNetworth().multiply(BigInteger.valueOf(MAX_BET_PERCENT)).divide(BigInteger.valueOf(100))
                    .max(HouseRules.MIN_BET).min(bot.getNetworth());
            long steps = cap.subtract(HouseRules.MIN_BET).divide(step).longValueExact();
            BigInteger bet = HouseRules.MIN_BET.add(step.multiply(BigInteger.valueOf(random.nextLong(steps + 1))));

            round.placeBet(bot, bet);
        }
    }

    Action decide(final RoundView view, final Player bot, final Round round)
    {
        RoundView.SeatView seat = view.seats().stream()
                .filter(s -> s.playerId().equals(bot.getUuid()))
                .findFirst()
                .orElseThrow();
        RoundView.HandView hand = seat.hands().get(seat.activeHand());
        return BasicStrategy.choose(hand, view.dealerCards().getFirst(), round.getLegalActions(bot));
    }
}
