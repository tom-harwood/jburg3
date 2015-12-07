package jburg;

import java.util.*;

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
    /** "Typedef" a map of PatternMatchers keyed by Nonterminal. */
    class PatternMap extends HashMap<Nonterminal, PatternMatcher<Nonterminal,NodeType>> {}
    /** "Typedef" a map of Closures by Nonterminal. */
    class ClosureMap    extends HashMap<Nonterminal, Closure<Nonterminal>> {}

    /**
     * This state's pattern matching productions.
     */
    private PatternMap  patternMatchers = new PatternMap();
    /**
     * Cost of each pattern match.
     */
    private CostMap     costMap = new CostMap();
    /**
     * This state's closures, i.e., nonterminal-to-nonterminal productions.
     */
    private ClosureMap  closures = new ClosureMap();

    private final NodeType  nodeType;

    State(NodeType nodeType)
    {
        this.nodeType = nodeType;
    }

    void setPatternMatcher(PatternMatcher<Nonterminal,NodeType> p, long cost)
    {
        assert(cost < getCost(p.target));
        costMap.put(p.target, cost);
        patternMatchers.put(p.target, p);
    }

    int size()
    {
        assert(patternMatchers.size() == costMap.size());
        return patternMatchers.size();
    }

    boolean isEmpty()
    {
        assert(patternMatchers.isEmpty() == costMap.isEmpty());
        return patternMatchers.isEmpty();
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

    Production<Nonterminal> getProduction(Nonterminal goal)
    {
        if (patternMatchers.containsKey(goal)) {
            return patternMatchers.get(goal);
        } else if (closures.containsKey(goal)) {
            return closures.get(goal);
        } else {
            throw new IllegalArgumentException(String.format("State %d cannot produce %s", number, goal));
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

    /**
     * Marshal nonterminals produced by both
     * pattern matchers and closures.
     */
    Set<Nonterminal> getNonterminals()
    {
        Set<Nonterminal> result = new HashSet<Nonterminal>();

        for (Nonterminal patternNonterminal: patternMatchers.keySet()) {
            result.add(patternNonterminal);
        }

        for (Nonterminal closureNonterminal: closures.keySet()) {
            assert !result.contains(closureNonterminal);
            result.add(closureNonterminal);
        }

        return result;
    }

    int getStateNumber()
    {
        return number;
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();

        buffer.append("State ");
        buffer.append(String.valueOf(number));
        buffer.append(" ");
        buffer.append(this.nodeType);

        if (patternMatchers.size() > 0) {
            buffer.append("(patterns(");

            boolean didFirst = false;
            for (Nonterminal nt: patternMatchers.keySet()) {
                PatternMatcher p = patternMatchers.get(nt);

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
     * @return an XML rendering of this state.
     */
    String xml()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(String.format("<state number=\"%d\" nodeType=\"%s\">", number, nodeType));

        if (patternMatchers.size() > 0) {
            buffer.append("<patterns>");

            for (Nonterminal nt: patternMatchers.keySet()) {
                PatternMatcher<Nonterminal, NodeType> p = patternMatchers.get(nt);
                buffer.append(String.format("<pattern nt=\"%s\" pattern=\"%s\"/>", nt, p));
            }
            buffer.append("</patterns>");
        }

        if (closures.size() > 0) {
            buffer.append("<closures>");
            for (Closure<Nonterminal> closure: closures.values()) {
                buffer.append(String.format("<closure nt=\"%s\" source=\"%s\"/>", closure.target, closure.source));
            }
            buffer.append("</closures>");
        }

        buffer.append("</state>");

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
        return nodeType.hashCode() * 31 + patternMatchers.hashCode();
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
            return this.nodeType.equals(s.nodeType) && this.patternMatchers.equals(s.patternMatchers);

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
