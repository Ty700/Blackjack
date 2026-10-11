package tech.ty700.blackjack.core;

import java.util.*;
import java.util.stream.IntStream;
import java.util.concurrent.ThreadLocalRandom;

public class Table {
    private final static int MAX_PLAYERS_PER_TABLE = 7;

    private final UUID id = UUID.randomUUID();
    private final Deck deck = new Deck();
    private final Dealer dealer = new Dealer();
    private final List<Player> participantsAtTable = new ArrayList<>(Collections.nCopies(this.MAX_PLAYERS_PER_TABLE, null));

    Table() {}

    public boolean tableFull() { return this.participantsAtTable.stream().noneMatch(Objects::isNull);
    }

    /* Random */
    public void playerConnect(final Player p)
    {
        if (tableFull()) {
            throw new IllegalStateException("Table is full");
        }

        final List<Integer> openSeats = IntStream.range(0, this.participantsAtTable.size())
                .filter(i -> this.participantsAtTable.get(i) == null)
                .boxed()
                .toList();

        int seat = openSeats.get(ThreadLocalRandom.current().nextInt(openSeats.size()));

        playerConnect(p, seat);
    }

    /* Specific slot */
    public void playerConnect(final Player p, final int slot)
    {
        if (tableFull()) {
            throw new IllegalStateException("Table is full");
        }

        if(participantsAtTable.get(slot) != null)
        {
            throw new IllegalArgumentException("Slot taken");
        }

        participantsAtTable.set(slot, p);
    }

    public void dealCards()
    {
        for (int round = 0; round < 2; round++)
        {
            for(Participant p: participantsAtTable)
            {
                if ( p == null) continue;

                p.hit(deck.drawCard()[0]);
            }
            this.dealer.hit(deck.drawCard()[0]);
        }
    }

    public void playerDisconnect(final Player p)
    {
        /* Save player */

        /* Remove player */
        int seatToRemove = this.participantsAtTable.indexOf(p);
        if ( seatToRemove != -1)
        {
            this.participantsAtTable.set(seatToRemove, null);
        }
    }
}
