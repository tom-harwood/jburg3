package jburg;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class Closure<Nonterminal>
{
    final Nonterminal   target;
    final Nonterminal   source;
    final long          ownCost;
    Method              postCallback;

    public Closure(Nonterminal target, Nonterminal source, Method postCallback, long ownCost)
    {
        this.target         = target;
        this.source         = source;
        this.ownCost        = ownCost;
        this.postCallback   = postCallback;
    }

    public Closure(Nonterminal target, Nonterminal source, Method postCallback)
    {
        this(target, source, postCallback, 1);
    }

    @Override
    public String toString()
    {
        return String.format("Closure %s = %s:%d", target, source, ownCost);
    }
}
