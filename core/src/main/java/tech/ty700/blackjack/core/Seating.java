package tech.ty700.blackjack.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Who sits where. One seat per player.
 */
class Seating
{
    static final int MAX_SEATS = 7;

    private final List<Player> seats = new ArrayList<>(Collections.nCopies(MAX_SEATS, null));

    boolean isFull()    { return this.seats.stream().noneMatch(p -> p == null); }
    boolean isEmpty()   { return this.seats.stream().allMatch(p -> p == null); }

    boolean contains(final Player p) { return seatOf(p) != -1; }

    int seatOf(final Player p)
    {
        for (int i = 0; i < this.seats.size(); i++)
        {
            Player seated = this.seats.get(i);
            if (seated != null && seated.getUuid().equals(p.getUuid())) return i;
        }
        return -1;
    }

    /* Random open seat */
    int sit(final Player p)
    {
        if (isFull())
        {
            throw new IllegalStateException("Table is full");
        }

        final List<Integer> openSeats = IntStream.range(0, this.seats.size())
                .filter(i -> this.seats.get(i) == null)
                .boxed()
                .toList();

        int seat = openSeats.get(ThreadLocalRandom.current().nextInt(openSeats.size()));
        sit(p, seat);
        return seat;
    }

    /* Specific seat */
    void sit(final Player p, final int seat)
    {
        if (seat < 0 || seat >= MAX_SEATS)
        {
            throw new IllegalArgumentException("Seat must be 0 to " + (MAX_SEATS - 1));
        }
        if (contains(p))
        {
            throw new IllegalStateException(p.getName() + " already has a seat");
        }
        if (this.seats.get(seat) != null)
        {
            throw new IllegalArgumentException("Seat taken");
        }

        this.seats.set(seat, p);
    }

    void leave(final Player p)
    {
        int seat = seatOf(p);
        if (seat != -1)
        {
            this.seats.set(seat, null);
        }
    }

    /* Seat number to player, in seat order */
    SortedMap<Integer, Player> occupied()
    {
        SortedMap<Integer, Player> occupied = new TreeMap<>();
        for (int i = 0; i < this.seats.size(); i++)
        {
            if (this.seats.get(i) != null) occupied.put(i, this.seats.get(i));
        }
        return occupied;
    }
}
