package jburg;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

class State<Nonterminal, NodeType>
{
    int number = -1;

    /** "Typedef" a map of costs by nonterminal. */
    class CostMap extends HashMap<Nonterminal,Long> {}
    /** "Typedef" a map of Productions by Nonterminal. */
    class ProductionMap extends HashMap<Nonterminal, Production<Nonterminal,NodeType>> {}
    /** "Typedef" a map of Closures by Nonterminal. */
    class ClosureMap    extends HashMap<Nonterminal, Closure<Nonterminal>> {}

    private CostMap         costMap = new CostMap();
    private ProductionMap   productions = new ProductionMap();
    private ClosureMap      closures = new ClosureMap();

    private final NodeType  nodeType;

    State(NodeType nodeType)
    {
        this.nodeType = nodeType;
    }

    void setProduction(Production<Nonterminal,NodeType> p, long cost)
    {
        assert(cost < getCost(p.target));
        costMap.put(p.target, cost);
        productions.put(p.target, p);
    }

    int size()
    {
        assert(productions.size() == costMap.size());
        return productions.size();
    }

    boolean empty()
    {
        assert(productions.isEmpty() == costMap.isEmpty());
        return productions.isEmpty();
    }

    long getCost(Nonterminal nt)
    {
        if (costMap.containsKey(nt)) {
            return costMap.get(nt);

        } else if (closures.containsKey(nt)) {
            Closure<Nonterminal> closure = closures.get(nt);
            long closedCost = closure.ownCost + getCost(closure.source);
            assert closedCost < Integer.MAX_VALUE;
            return closedCost;

        } else {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Add a closure to the closure map if it's the best alternative seen so far.
     * @return true if the closure is added to the map.
     */
    boolean addClosure(Closure<Nonterminal> closure)
    {
        // The cost of a closure is its own cost,
        // plus the cost of producing its antecedent.
        long closureCost = closure.ownCost + getCost(closure.source);

        if (closureCost < this.getCost(closure.target)) {
            closures.put(closure.target, closure);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();

        buffer.append("State ");
        buffer.append(String.valueOf(number));

        if (productions.size() > 0) {
            buffer.append("( patterns(");

            boolean didFirst = false;
            for (Nonterminal nt: productions.keySet()) {
                Production p = productions.get(nt);

                if (didFirst) {
                    buffer.append(",");
                } else {
                    didFirst = true;
                }
                buffer.append(String.format("%s=%s", nt, p));
            }
            buffer.append(")");
            if (closures.size() > 0) {
                buffer.append(closures);
            }
            buffer.append(")");
        }

        return buffer.toString();
    }

    /**
     * Define a state's hash code in terms of its cost map's hash code.
     * @return this state's cost map's hashCode()
     */
    @Override
    public int hashCode()
    {
        return costMap.hashCode();
    }

    /**
     * Two states are equal if their cost maps are equal.
     * @param o the object to compare against.
     * @return unclosed.costMap(o.costMap) if o is a State,
     * false otherwise.
     */
    @Override
    public boolean equals(Object o)
    {
        return o instanceof State?
            this.costMap.equals(((State)o).costMap):
            false;
    }
}
