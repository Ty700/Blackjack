package tech.ty700.blackjack.cli;

import tech.ty700.blackjack.core.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

/**
 * Draws the whole table every time: dealer in the middle up top, the 7 seats in a row underneath,
 * then the status line and messages. The screen is wiped before each draw so it stays in one place.
 *
 *  [K♣]  a face-up card
 *  [?]   a card dealt face down
 *  [ ]   an empty spot (no card yet, or nobody in the seat)
 */
class TableScreen
{
    private static final int SEATS = 7;
    private static final int COLUMN_WIDTH = 16;
    private static final int CARDS_PER_ROW = 3;
    private static final int WIDTH = SEATS * COLUMN_WIDTH + (SEATS - 1);

    private static final String CLEAR = "\033[H\033[2J";

    /* A hand still being dealt has no total to show yet */
    static final int UNKNOWN_TOTAL = -1;
    private static final String EMPTY_SPOTS = "[ ][ ]";

    /* Short names for huge numbers so they fit a column. 10^3 up to 10^63 (vigintillion) */
    private static final String[] UNITS = {
            "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No",
            "Dc", "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod", "Vg" };

    /**
     * view: the round to show, or null for a bare table.
     * seated: who is sitting where right now (people who sat down mid-round still show up).
     */
    void draw(final RoundView view, final SortedMap<Integer, Player> seated, final Player me,
              final String status, final List<String> messages)
    {
        List<String> out = new ArrayList<>();
        out.add("");
        drawDealer(view, out);
        out.add("");
        drawSeats(view, seated, me, out);
        out.add("");
        out.add("-".repeat(WIDTH));
        out.add(status);
        out.addAll(messages);
        out.add("");

        IO.print(CLEAR + String.join("\n", out) + "\n");
        System.out.flush();
    }

    /* ---------- Dealer ---------- */

    private void drawDealer(final RoundView view, final List<String> out)
    {
        out.add(center("DEALER", WIDTH));

        if (view == null || view.dealerCards().isEmpty())
        {
            out.add(center(EMPTY_SPOTS, WIDTH));
            out.add("");
            return;
        }

        String cards = cards(view.dealerCards()) + "[?]".repeat(view.dealerHiddenCards());
        out.add(center(cards, WIDTH));
        out.add(center(view.dealerTotal() == null ? "" : "(" + view.dealerTotal() + ")", WIDTH));
    }

    /* ---------- Seats ---------- */

    private void drawSeats(final RoundView view, final SortedMap<Integer, Player> seated, final Player me,
                           final List<String> out)
    {
        List<List<String>> columns = new ArrayList<>();
        for (int seat = 0; seat < SEATS; seat++)
        {
            columns.add(seatColumn(seat, view, seated.get(seat), me));
        }

        int height = columns.stream().mapToInt(List::size).max().orElse(0);
        for (int row = 0; row < height; row++)
        {
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < SEATS; c++)
            {
                if (c > 0) line.append(' ');
                List<String> column = columns.get(c);
                line.append(pad(row < column.size() ? column.get(row) : "", COLUMN_WIDTH));
            }
            out.add(line.toString().stripTrailing());
        }
    }

    private List<String> seatColumn(final int seat, final RoundView view, final Player player, final Player me)
    {
        List<String> column = new ArrayList<>();
        RoundView.SeatView seatView = findSeat(view, seat);
        boolean isMe = player != null && player.getUuid().equals(me.getUuid());
        boolean myTurn = seatView != null && seatView.activeHand() != null;

        /* Header */
        String name = player == null ? "Seat " + (seat + 1) : player.getName().toUpperCase();
        if (myTurn) name = "> " + name + " <";
        column.add(center(name, COLUMN_WIDTH));
        column.add(center(isMe ? "(you)" : "", COLUMN_WIDTH));

        /* Nobody here */
        if (player == null)
        {
            column.add(center(EMPTY_SPOTS, COLUMN_WIDTH));
            column.add(center("empty", COLUMN_WIDTH));
            return column;
        }

        /* Seated but not playing this round */
        if (seatView == null || seatView.bet() == null)
        {
            column.add(center(EMPTY_SPOTS, COLUMN_WIDTH));
            column.add(center(seatView != null && view.phase() == Round.Phase.BETTING ? "betting..." : "sitting out", COLUMN_WIDTH));
            return column;
        }

        /* Bet placed, cards not dealt yet */
        if (seatView.hands().isEmpty())
        {
            column.add(center(EMPTY_SPOTS, COLUMN_WIDTH));
            column.add(center("bet " + compact(seatView.bet()), COLUMN_WIDTH));
            return column;
        }

        for (int i = 0; i < seatView.hands().size(); i++)
        {
            RoundView.HandView hand = seatView.hands().get(i);
            /* The header already shows whose turn it is, so only mark the hand after a split */
            boolean active = seatView.hands().size() > 1 && seatView.activeHand() != null && seatView.activeHand() == i;
            if (i > 0) column.add("");

            for (String row : cardRows(hand.cards()))
            {
                column.add(center(row, COLUMN_WIDTH));
            }

            String total = hand.total() == UNKNOWN_TOTAL ? "" : (active ? "> " : "") + total(hand) + resultTag(hand);
            column.add(center(total, COLUMN_WIDTH));
            column.add(center("bet " + compact(hand.bet()), COLUMN_WIDTH));
        }
        return column;
    }

    /* ---------- Formatting ---------- */

    private static RoundView.SeatView findSeat(final RoundView view, final int seat)
    {
        if (view == null) return null;
        for (RoundView.SeatView s : view.seats())
        {
            if (s.seat() == seat) return s;
        }
        return null;
    }

    private static List<String> cardRows(final List<Card> cards)
    {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < cards.size(); i += CARDS_PER_ROW)
        {
            rows.add(cards(cards.subList(i, Math.min(i + CARDS_PER_ROW, cards.size()))));
        }
        return rows;
    }

    private static String cards(final List<Card> cards)
    {
        StringBuilder sb = new StringBuilder();
        for (Card c : cards)
        {
            sb.append('[').append(c.displayString()).append(']');
        }
        return sb.toString();
    }

    private static String total(final RoundView.HandView hand)
    {
        return hand.soft() && hand.total() < 21 ? "soft " + hand.total() : String.valueOf(hand.total());
    }

    private static String resultTag(final RoundView.HandView hand)
    {
        return switch (hand.result())
        {
            case IN_PROGRESS -> "";
            case BLACKJACK -> " BJ!";
            case WON -> " WIN";
            case LOST -> " LOSE";
            case PUSHED -> " PUSH";
            case BUSTED -> " BUST";
        };
    }

    /* 1,500 / 250K / 3.4M ... / 1.2Vg / 4.5e70 */
    static String compact(final BigInteger amount)
    {
        if (amount.abs().compareTo(BigInteger.valueOf(100_000)) < 0)
        {
            return String.format("%,d", amount);
        }

        String sign = amount.signum() < 0 ? "-" : "";
        String digits = amount.abs().toString();
        int groups = (digits.length() - 1) / 3;
        int leading = digits.length() - groups * 3;
        String number = digits.substring(0, leading) + "." + digits.charAt(leading);

        if (groups <= UNITS.length) return sign + number + UNITS[groups - 1];
        return sign + digits.charAt(0) + "." + digits.charAt(1) + "e" + (digits.length() - 1);
    }

    private static String center(final String s, final int width)
    {
        if (s.length() >= width) return s.substring(0, width);
        int left = (width - s.length()) / 2;
        return " ".repeat(left) + s;
    }

    private static String pad(final String s, final int width)
    {
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }
}
