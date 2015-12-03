package jburg;

import java.util.*;

// FIXME: Need an abstract superclass, e.g., Item
class RepresenterState<Nonterminal,NodeType> extends State<Nonterminal,NodeType>
{
    Map<Nonterminal, Long> costMap = new HashMap<Nonterminal, Long>();

    RepresenterState(NodeType type)
    {
        super(type);
    }

    void setCost(Nonterminal nt, long cost)
    {
        assert(getCost(nt) < cost);
        costMap.put(nt,cost);
    }

    long getCost(Nonterminal nt)
    {
        return costMap.containsKey(nt)? costMap.get(nt): Integer.MAX_VALUE;
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
