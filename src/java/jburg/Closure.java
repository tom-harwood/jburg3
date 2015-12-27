package jburg;

import java.lang.reflect.Method;

/**
 * A Closure represents a nonterminal-nonterminal transformation.
 * Like a PatternMatcher, it can have pre and post callbacks.
 */
public class Closure<Nonterminal> extends Production<Nonterminal>
{
    final Nonterminal   source;

    public Closure(Nonterminal target, Nonterminal source, int ownCost, Method postCallback)
    {
        super(target, ownCost, false, null, null, postCallback);
        this.source = source;
    }

    public Closure(Nonterminal target, Nonterminal source, Method postCallback)
    {
        this(target, source, 1, postCallback);
    }

    @Override
    public String toString()
    {
        return String.format("Closure %s = %s:%d", target, source, ownCost);
    }
}
