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
    NodeType                nodeType;
    Map<Nonterminal, Long>  costMap = new HashMap<Nonterminal, Long>();

    RepresenterState(NodeType nodeType)
    {
        this.nodeType = nodeType;
    }

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
        return nodeType.hashCode() * 31 + costMap.hashCode();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public boolean equals(Object x)
    {
        if (x instanceof RepresenterState) {
            RepresenterState<Nonterminal,NodeType> rs = (RepresenterState<Nonterminal,NodeType>)x;
            return this.nodeType.equals(rs.nodeType) && this.costMap.equals(rs.costMap);
        } else {
            return false;
        }
    }

    @Override
    public String toString()
    {
        return costMap.toString();
    }
}
