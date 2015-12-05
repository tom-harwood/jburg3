package jburg;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A State represents a vertex in the transition table.
 * Vertices represent an equivalence class of input nodes,
 * each of which has the same opcode/arity; an input node
 * must match one of the pattern-matching productions in
 * the state. The state may also be able to produce other
 * nonterminals via nonterminal-to-nonterminal closures.
 *
 * The State class is Comparable so that it can be
 * sorted on its state number.
 */
class State<Nonterminal, NodeType> implements Comparable<State<Nonterminal,NodeType>>
{
    /**
     * The state's number. This number is set
     * by the production table when it places
     * a state in its table of unique states.
     */
    int number = -1;

    /** "Typedef" a map of costs by nonterminal. */
    class CostMap extends HashMap<Nonterminal,Long> {}
    /** "Typedef" a map of Productions by Nonterminal. */
    class ProductionMap extends HashMap<Nonterminal, PatternMatcher<Nonterminal,NodeType>> {}
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

    void setPatternMatcher(PatternMatcher<Nonterminal,NodeType> p, long cost)
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

    boolean isEmpty()
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
        buffer.append(" ");
        buffer.append(this.nodeType);

        if (productions.size() > 0) {
            buffer.append("(patterns(");

            boolean didFirst = false;
            for (Nonterminal nt: productions.keySet()) {
                PatternMatcher p = productions.get(nt);

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
     * Define a state's hash code in terms of its
     * node type's hash code and its production map's
     * hash code. Note that using the cost map's
     * hash code will not work, since subsequent
     * iterations may produce states that are identical
     * except that they cost more due to closures, so
     * that computation can run away.
     * @return this state's node type's hashCode(),
     * concatenated with the production map's hashCode().
     */
    @Override
    public int hashCode()
    {
        return nodeType.hashCode() * 31 + productions.hashCode();
    }

    /**
     * Two states are equal if their node types
     * and production maps are equal.
     * @param o the object to compare against.
     * @return unclosed.costMap(o.costMap) if o is a State,
     * false otherwise.
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public boolean equals(Object o)
    {
        if (o instanceof State) {
            State<Nonterminal,NodeType> s = (State<Nonterminal,NodeType>)o;
            return this.nodeType.equals(s.nodeType) && this.productions.equals(s.productions);

        } else {
            return false;
        }
    }

    /**
     * States are comparable on their state number;
     * this is a convenience so that the state table,
     * which is stored in hashed order, can be emitted
     * in state number order.
     */
    @Override
    public int compareTo(State<Nonterminal,NodeType> other)
    {
        assert this.number != -1 && other.number != -1;
        return this.number - other.number;
    }
}
