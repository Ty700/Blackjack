package tech.ty700.blackjack.core;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;

/**
 * The overall manager: seats, the shoe, the dealer, and the current round.
 * Not thread-safe on purpose. The server runs each table on one thread.
 */
public class Table {
    private final UUID id = UUID.randomUUID();
    private final Dealer dealer;
    private final Deck deck;
    private final Seating seating = new Seating();
    private final HouseLedger ledger = new HouseLedger();
    private Round currentRound = null;

    /* The server hands over a dealer from the pool. The CLI can just make one */
    public Table(final Dealer aDealer)
    {
        this(aDealer, new Deck());
    }

    Table(final Dealer aDealer, final Deck aDeck)
    {
        this.dealer = aDealer;
        this.deck = aDeck;
    }

    public UUID getId()         { return this.id; }
    public Dealer getDealer()   { return this.dealer; }
    public boolean isFull()     { return this.seating.isFull(); }
    /* The server closes empty tables and returns the dealer to the pool */
    public boolean isEmpty()    { return this.seating.isEmpty(); }

    public SortedMap<Integer, Player> getSeatedPlayers()
    {
        return Collections.unmodifiableSortedMap(this.seating.occupied());
    }

    /* Random open seat. Returns the seat number */
    public int playerConnect(final Player p)
    {
        return this.seating.sit(p);
    }

    /* Specific seat */
    public void playerConnect(final Player p, final int seat)
    {
        this.seating.sit(p, seat);
    }

    public void playerDisconnect(final Player p)
    {
        /* Save player: the server does this after every round and on leave */
        if (this.currentRound != null) this.currentRound.playerLeft(p);
        this.seating.leave(p);
    }

    /* ---------- Rounds ---------- */

    public Round startRound()
    {
        if (isRoundInProgress())
        {
            throw new IllegalStateException("A round is already in progress");
        }
        this.currentRound = new Round(this.seating.occupied(), this.dealer, this.deck, this.ledger);
        return this.currentRound;
    }

    public Optional<Round> getCurrentRound()    { return Optional.ofNullable(this.currentRound); }

    public boolean isRoundInProgress()
    {
        return this.currentRound != null && !this.currentRound.isOver();
    }

    /* ---------- Between rounds ---------- */

    /* Early loan repayment. Allowed between rounds and during betting */
    public BigInteger repayLoan(final Player p, final BigInteger amount)
    {
        requireSeated(p);
        requireBetweenRounds();
        BigInteger paid = p.repay(amount);
        this.ledger.add(paid);
        return paid;
    }

    public void prestige(final Player p)
    {
        requireSeated(p);
        requireBetweenRounds();
        if (this.currentRound != null && this.currentRound.hasBet(p))
        {
            throw new IllegalStateException("Can't prestige with a bet on the table");
        }
        p.prestige();
    }

    /* Repayments waiting for the global house total. The server adds them to the DB atomically */
    public BigInteger drainHouseRepayments()
    {
        return this.ledger.drain();
    }

    private void requireSeated(final Player p)
    {
        if (!this.seating.contains(p))
        {
            throw new IllegalStateException(p.getName() + " is not at this table");
        }
    }

    private void requireBetweenRounds()
    {
        if (isRoundInProgress() && this.currentRound.getPhase() != Round.Phase.BETTING)
        {
            throw new IllegalStateException("Only allowed between rounds");
        }
    }
}
