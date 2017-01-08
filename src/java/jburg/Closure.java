package jburg;

import jburg.semantics.HostRoutine;

/**
 * A Closure represents a nonterminal-nonterminal transformation.
 * Like a PatternMatcher, it can have pre and post callbacks.
 */
public class Closure<Nonterminal> extends Production<Nonterminal>
{
    final  String   source;
    public Object   getSource() { return source; }


    public Closure(Object target, Object source, int ownCost, HostRoutine preCallback, HostRoutine postCallback)
    {
        super(target.toString(), ownCost, false, null, preCallback, postCallback);
        this.source = source.toString();
    }

    public Closure(Object target, Object source, int ownCost, HostRoutine postCallback)
    {
        this(target, source, ownCost, null, postCallback);
    }

    public Closure(Object target, Object source, HostRoutine postCallback)
    {
        this(target, source, 1, null, postCallback);
    }

    @Override
    public String toString()
    {
        return String.format("Closure %s = %s:%d", target, source, ownCost);
    }
}
