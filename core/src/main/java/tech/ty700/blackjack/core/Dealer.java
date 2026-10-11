package tech.ty700.blackjack.core;

import java.util.UUID;
import java.math.BigInteger;

public class Dealer extends Participant
{
    Dealer()
    {
        super("Dealer", UUID.randomUUID(), BigInteger.ZERO);
    }

    @Override
    public void turn() {}
}