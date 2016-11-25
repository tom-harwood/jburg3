package jburg;

import jburg.semantics.HostRoutine;

/**
 * A ErrorHandlerProduction represents a
 * derivation of a null subtree.
 */
public class ErrorHandlerProduction<Nonterminal> extends Production<Nonterminal>
{
    public ErrorHandlerProduction(Nonterminal target, HostRoutine errorHandler)
    {
        super(target, 1, false, null, errorHandler, null);
    }

    @Override
    public String toString()
    {
        return String.format("ErrorHandlerProduction %s cost:%s %s)", target, ownCost, getCallbackName(postCallback));
    }
}
