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
 * <p>Store State objects in hashed associative containers;
 * State objects' hash and equality semantics are set up
 * to weed out duplicate states.
 */
class State<Nonterminal, NodeType>
{
    /**
     * The state's number. This number is set
     * by the production table when it places
     * a state in its table of unique states.
     */
    int number = -1;

    /** "Typedef" a map of costs by nonterminal. */
    @SuppressWarnings("serial")
	class CostMap extends HashMap<Nonterminal,Long> {}
    /** "Typedef" a map of PatternMatchers keyed by Nonterminal. */
    @SuppressWarnings("serial")
	class PatternMap extends HashMap<Nonterminal, PatternMatcher<Nonterminal,NodeType>> {}
    /** "Typedef" a map of Closures by Nonterminal. */
    @SuppressWarnings("serial")
	class ClosureMap    extends HashMap<Nonterminal, Closure<Nonterminal>> {}

    /**
     * This state's pattern matching productions.
     */
    private PatternMap  patternMatchers = new PatternMap();
    /**
     * Cost of each pattern match.
     */
    private CostMap     patternCosts = new CostMap();
    /**
     * This state's closures, i.e., nonterminal-to-nonterminal productions.
     */
    private ClosureMap  closures = new ClosureMap();

    /**
     * The node type of this state; used while projecting
     * representer states, which are unique for a particular
     * tuple of (NodeType, nt=cost*).
     */
    final NodeType  nodeType;

    /**
     * Construct a state that characterizes non-null nodes.
     * @param nodeType the node type of the nodes.
     */
    State(NodeType nodeType)
    {
        this.nodeType = nodeType;
    }

    /**
     * Construct a state that characterizes null pointers.
     */
    State()
    {
        this.nodeType = null;
    }

    /**
     * Add a pattern match to this state.
     * @param p     the pattern matching production.
     * @param cost  the cost of this production. The
     * cost must be the best cost known so far.
     */
    void setPatternMatcher(PatternMatcher<Nonterminal,NodeType> p, long cost)
    {
        assert(cost < getCost(p.target));
        patternCosts.put(p.target, cost);
        patternMatchers.put(p.target, p);
    }

    /**
     * @return the number of pattern matching productions in this state.
     */
    int size()
    {
        assert(patternMatchers.size() == patternCosts.size());
        return patternMatchers.size();
    }

    /**
     * @return true if this state has no pattern matching productions.
     */
    boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * Get the cost of a nonterminal; this may require
     * navigation of a chain of closure productions back
     * to the pattern-matching production.
     * @return the aggregated cost of productions that
     * produce the given nonterminal, or Integer.MAX_VALUE
     * if there is no production for this nonterminal.
     * Costs are returned as longs (and computed as longs)
     * so that they don't overflow.
     */
    long getCost(Nonterminal nt)
    {
        if (patternCosts.containsKey(nt)) {
            return patternCosts.get(nt);

        } else if (closures.containsKey(nt)) {
            // Traverse the chain of closures.
            Closure<Nonterminal> closure = closures.get(nt);
            long closedCost = closure.ownCost + getCost(closure.source);
            assert closedCost < Integer.MAX_VALUE;
            return closedCost;

        } else {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Get the Production for a nonterminal.
     * @param goal  the Nonterminal to be produced.
     * @return the corresponding Production, which
     * may be a pattern matcher or a closure.
     * @throws IllegalArgumentException if this state
     * has no production for the specified nonterminal.
     */
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
     * @return the set of nonterminals produced.
     */
    Set<Nonterminal> getNonterminals()
    {
        // We could use a cheaper data structure, e.g.,
        // List<Nonterminal>, but returning a set makes
        // the semantics of this operation clear.
        Set<Nonterminal> result = new HashSet<Nonterminal>();

        for (Nonterminal patternNonterminal: patternMatchers.keySet()) {
            result.add(patternNonterminal);
        }

        for (Nonterminal closureNonterminal: closures.keySet()) {
            // A closure should never occlude a pattern match.
            assert !result.contains(closureNonterminal);
            result.add(closureNonterminal);
        }

        return result;
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
                PatternMatcher<Nonterminal, NodeType> p = patternMatchers.get(nt);

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
     * Dump an XML rendering of this state.
     * @param out   the output sink.
     */
    void dump(java.io.PrintWriter out)
    throws java.io.IOException
    {
        out.printf("<state number=\"%d\" nodeType=\"%s\">", number, nodeType);

        if (patternMatchers.size() > 0) {
            out.println("<patterns>");

            for (Nonterminal nt: patternMatchers.keySet()) {
                PatternMatcher<Nonterminal, NodeType> p = patternMatchers.get(nt);
                out.printf("<pattern nt=\"%s\" pattern=\"%s\"/>\n", nt, p);
            }
            out.printf("</patterns>");
        }

        if (closures.size() > 0) {
            out.println("<closures>");
            for (Closure<Nonterminal> closure: closures.values()) {
                out.printf(String.format("<closure nt=\"%s\" source=\"%s\"/>", closure.target, closure.source));
            }
            out.println("</closures>");
        }

        out.println("</state>");
    }

    /**
     * Dump an abbreviated rendering of this state.
     * @param out   the output sink.
     */
    void miniDump(java.io.PrintWriter out)
    throws java.io.IOException
    {
        out.printf("<leaf state=\"%d\"/>\n", number);
    }

    /**
     * Define a state's hash code in terms of its
     * node type's hash code and its pattern map's
     * hash code.
     *
     * <p> <strong>Using the cost map's hash code is invalid,</strong>
     * since subsequent iterations may produce states that
     * are identical except that they cost more due to closures,
     * so computations based on the cost map diverge.
     *
     * <p>However, two states with the same pattern map
     * will also have the same cost map after closure,
     * so the pattern map is a valid choice for hashing.
     *
     * @return this state's node type's hashCode(),
     * concatenated with the pattern map's hashCode().
     */
    @Override
    public int hashCode()
    {
        int nodeHash = nodeType != null? nodeType.hashCode(): 0;
        return nodeHash * 31 + patternMatchers.hashCode();
    }

    /**
     * Two states are equal if their node types
     * and pattern maps are equal.
     * @param o the object to compare against.
     * @return true if o is a State and its node
     * type and pattern map are equal to this
     * state's corresponding members; false otherwise.
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public boolean equals(Object o)
    {
        if (o instanceof State) {
            State<Nonterminal,NodeType> s = (State<Nonterminal,NodeType>)o;

            if (this.nodeType == s.nodeType) {
                return true;
            } else if (this.nodeType != null && s.nodeType != null) {
                return this.nodeType.equals(s.nodeType) && this.patternMatchers.equals(s.patternMatchers);
            }

        }

        return false;
    }
}
