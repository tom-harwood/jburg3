package jburg;

import jburg.semantics.HostRoutine;

/**
 * A Closure represents a nonterminal-nonterminal transformation.
 * Like a PatternMatcher, it can have pre and post callbacks.
 */
public class Closure<Nonterminal> extends Production<Nonterminal>
{
    final Nonterminal   source;
    public Nonterminal getSource() { return source; }


    public Closure(Nonterminal target, Nonterminal source, int ownCost, HostRoutine preCallback, HostRoutine postCallback)
    {
        super(target, ownCost, false, null, preCallback, postCallback);
        this.source = source;
    }

    public Closure(Nonterminal target, Nonterminal source, int ownCost, HostRoutine postCallback)
    {
        this(target, source, ownCost, null, postCallback);
    }

    public Closure(Nonterminal target, Nonterminal source, HostRoutine postCallback)
    {
        this(target, source, 1, null, postCallback);
    }

    @Override
    public String toString()
    {
        return String.format("Closure %s = %s:%d", target, source, ownCost);
    }
}
