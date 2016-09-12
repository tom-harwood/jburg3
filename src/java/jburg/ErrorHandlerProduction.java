package jburg;

import java.lang.reflect.Method;

/**
 * A ErrorHandlerProduction represents a
 * derivation of a null subtree.
 */
public class ErrorHandlerProduction<Nonterminal> extends Production<Nonterminal>
{
    public ErrorHandlerProduction(Nonterminal target, Method errorHandler)
    {
        super(target, 1, false, null, errorHandler, null);
    }

    @Override
    public String toString()
    {
        return String.format("ErrorHandlerProduction %s cost:%s %s)", target, ownCost, getCallbackName(postCallback));
    }
}
