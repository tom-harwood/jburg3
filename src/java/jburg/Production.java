package jburg;

import java.lang.reflect.Method;

/**
 * A Production represents a transformation
 * of an input to an output nonterminal,
 * either by directly matching a pattern to
 * the initial input (a PatternMatcher) or by 
 * further transforming an initial nonterminal
 * to a new nonterminal (a Closure).
 */
abstract class Production<Nonterminal>
{
    public final Nonterminal    target;
    public final int            ownCost;
    public final Method         preCallback;
    public final Method         postCallback;

    Production(Nonterminal target, int ownCost, Method preCallback, Method postCallback)
    {
        this.target         = target;
        this.ownCost        = ownCost;
        this.preCallback    = preCallback;
        this.postCallback   = postCallback;
    }

    protected String getCallbackName(Method callback)
    {
        if (callback != null) {
            if (callback == this.preCallback) {
                return String.format("preCallback:%s", callback.getName());
            } else if (callback == this.postCallback) {
                return String.format("postCallback:%s", callback.getName());
            } else {
                throw new IllegalStateException(String.format("Unknown callback method %s", callback));
            }
        } else {
            return "";
        }
    }
}
