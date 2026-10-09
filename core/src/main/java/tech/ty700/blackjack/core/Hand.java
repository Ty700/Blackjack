package tech.ty700.blackjack.core;

import java.util.ArrayList;
import java.util.List;

public class Hand
{
    /* Reserver 6 cards per hand */
    private final List<Card> hand = new ArrayList<>(6);

    public enum HandState   { IDLE, IN_PROGRESS, DONE }
    public enum HandResult  { IN_PROGRESS, BUSTED, PUSHED, LOST, WON, BLACKJACK }

    /* Members */
    private int         handTotal = 0;
    private long        betToHand = 0;
    private HandState   handState = HandState.IDLE;
    private HandResult  handResults = HandResult.IN_PROGRESS;

    /* Dealer */
    /* Why? Throw logic for aBet... dealer doesn't have a bet */
    Hand() {}

    /* Players */
    Hand(final long aBet )
    {
        if(aBet <= 0)
        {
            throw new IllegalArgumentException("Bet must be positive");
        }

        this.betToHand = aBet;
    }

    /* Setters */
    /* Called when dealer determine if hand won/lost */
    public void setHandResult(final HandResult r) {
        if (this.handResults != HandResult.IN_PROGRESS)
        {
            throw new IllegalStateException("Can only assign results to a hand that is in-progress, or a state has already been set");
        }
        this.handResults = r;
    }

    /* Getters */
    public HandResult getHandResult()   { return this.handResults;  }
    public long getBetTotal()           { return this.betToHand;    }
    public int getHandTotal()           { return this.handTotal;    }
    public HandState getHandState()     { return this.handState;    }
    /* Returns a copy so caller can't bypass addCard */
    public List<Card> getHand() { return List.copyOf(this.hand); }

    /* Determinations */
    public boolean canHit()
    {
        return (this.handState == HandState.IN_PROGRESS) && this.handTotal <= 21;
    }

    public boolean canSplit()
    {
        if (this.handState != HandState.IN_PROGRESS) return false;

        /* Can only split on first two cards */
        if (this.hand.size() != 2) return false;

        /* 4 != 5 */
        if (this.hand.get(0).getRankValue() != this.hand.get(1).getRankValue()) return false;

        /* J/Q/K can't split with 10*/
        /* K/J can split with Q */
        /* J can split with K */
        return (this.hand.get(0).rank() == Rank.TEN) == (this.hand.get(1).rank() == Rank.TEN);
    }

    /* Hand (state or count) altering */
    public void addCard(Card aCard) {
        if (this.handState == HandState.DONE) {
            /* TODO: Save the card being dealt */
            throw new IllegalStateException("Hand is finished");
        }

        if (this.handState != HandState.IN_PROGRESS) {
            this.handState = HandState.IN_PROGRESS;
        }

        this.hand.add(aCard);
        calculateHandTotal();
    }

    public void stand()
    {
        if (this.handState != HandState.IN_PROGRESS)
        {
            throw new IllegalStateException("Can only stand on in-progress hands");
        }

        this.handState = HandState.DONE;
    }

    public void doubleHand(final Card c, final long anAdditionalBet)
    {
        if (this.hand.size() != 2 || this.handState != HandState.IN_PROGRESS) {
            throw new IllegalStateException("Can only double on first two cards of an in-progress hand.");
        }

        /* Ownus is on the game to pass bet amount... */
        /* Most time this is just *2 of bet.. but perhaps there are power ups that allow for different amounts */
        this.betToHand += anAdditionalBet;
        addCard(c);
        this.handState = HandState.DONE;
    }

    /* Helpers */
    private void calculateHandTotal() {
        int sum = 0;
        int aces = 0;
        for (Card c : this.hand) {
            /* Every ace starts out counted as 11 */
            if (c.rank() == Rank.ACE) {
                aces += 1;
            }
            sum += c.getRankValue();
        }

        /* Drop aces from 11 to 1, one at a time, until the hand is no longer bust */
        while (sum > 21 && aces > 0) {
            sum -= 10;
            aces -= 1;
        }

        /* Busted? */
        if (sum > 21) {
            this.handState = HandState.DONE;
            this.handResults = HandResult.BUSTED;
        }

        if (sum == 21 && this.hand.size() == 2) {
            this.handState = HandState.DONE;
            this.handResults = HandResult.BLACKJACK;
        }

        this.handTotal = sum;
    }
}
