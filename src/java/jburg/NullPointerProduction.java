package jburg;

import jburg.semantics.HostRoutine;

/**
 * A NullPointerProduction represents a
 * derivation of a null subtree.
 */
public class NullPointerProduction<Nonterminal> extends Production<Nonterminal>
{
    public NullPointerProduction(Nonterminal target, int cost, HostRoutine postCallback)
    {
        super(target, cost, false, null, null, postCallback);
    }

    @Override
    public String toString()
    {
        return String.format("NullPointerProduction %s cost:%s %s)", target, ownCost, getCallbackName(postCallback));
    }
}
