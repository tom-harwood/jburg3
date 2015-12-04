package jburg;

import java.util.*;

/**
 * A RepresenterState is a "skeleton" representation
 * of a State; many states may map to the same RepresenterState.
 * The transition tables are keyed by tuples of RepresenterState
 * objects, which effects a very significant compaction of the
 * resulting transition table.
 */
class RepresenterState<Nonterminal,NodeType>
{
    Map<Nonterminal, Long> costMap = new HashMap<Nonterminal, Long>();

    void setCost(Nonterminal nt, long cost)
    {
        assert(cost < getCost(nt));
        costMap.put(nt,cost);
    }

    long getCost(Nonterminal nt)
    {
        return costMap.containsKey(nt)? costMap.get(nt): Integer.MAX_VALUE;
    }

    boolean isEmpty()
    {
        return costMap.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return costMap.hashCode();
    }

    @Override
    public boolean equals(Object x)
    {
        return costMap.equals(x);
    }

    @Override
    public String toString()
    {
        return costMap.toString();
    }
}
