package jburg;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

class State<Nonterminal, NodeType>
{
    int number = -1;

    private CostMap         unclosed = new CostMap();
    private CostMap         closed = null;
    private ClosureMap      closureMap = new ClosureMap();

    private final NodeType  nodeType;

    State(NodeType nodeType)
    {
        this.nodeType = nodeType;
    }

    void setProduction(Production<Nonterminal,NodeType> p, long cost)
    {
        verifyModifiable();
        assert(cost < getCost(p.target));
        unclosed.put(p.target, p);
    }

    int size()
    {
        return unclosed.size();
    }

    boolean empty()
    {
        return unclosed.isEmpty();
    }

    long getCost(Nonterminal nt)
    {
        CostMap current = getCurrent();
        return current.containsKey(nt)? current.getCost(nt): Integer.MAX_VALUE;
    }

    private CostMap getCurrent()
    {
        return closed != null? closed: unclosed;
    }

    /**
     * Add a closure to the closure map, but
     * don't create the closed cost map.
     * @return true if the closure is new.
     */
    boolean addClosure(Closure<Nonterminal> closure)
    {
        verifyModifiable();

        // The production table checked that there was no pattern
        // match for this closure, but can't check whether there
        // is a better closure.
        if (closure.ownCost < closureMap.getCost(closure.target)) {
            closureMap.put(closure.target, closure);
            return true;
        } else {
            return false;
        }
    }

    private void verifyModifiable()
    {
        if (closed != null) {
            throw new IllegalStateException(String.format("State not modifiable: %s", toString()));
        }
    }

    // TODO: @SafeVarargs would be a better annotation,
    // but that would require Java 1.7 or above.
    @SuppressWarnings({"unchecked"})
    void finish()
    {
        verifyModifiable();

        this.closed = new CostMap(this.unclosed);
        int finishedSize = unclosed.size() + closureMap.size();

        // Remove items from the closure map
        // in dependency order; on each pass
        // through the map, there must be a
        // Closure whose antecedent is already
        // in the map of closed productions.
        // Create a production for that closure
        // and add it to the closed productions.
        while (!closureMap.isEmpty()) {
            boolean processedClosure = false;

            for (Closure<Nonterminal> closure: closureMap.values()) {

                // TODO: elide trivial callbacks.
                if (closed.containsKey(closure.source)) {

                    assert(closed.size() < finishedSize);

                    closed.put(closure.target,
                        new Production<Nonterminal,NodeType>(
                            closure.target,
                            this.nodeType,
                            (int)closure.ownCost,
                            closure.postCallback,
                            closed.get(closure.source)
                        )
                    );
                    closureMap.remove(closure.target);
                    processedClosure = true;
                    break;
                }
            }

            assert(processedClosure);
        }
    }

    void setProduction(Nonterminal nt, Production<Nonterminal,NodeType> p)
    {
        verifyModifiable();
        unclosed.put(nt, p);
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();


        if (number != -1) {
            buffer.append("State ");
            buffer.append(String.valueOf(number));
        } else {
            buffer.append("RepState");
        }

        if (getCurrent().size() > 0) {
            buffer.append("(");
            boolean didFirst = false;
            for (Nonterminal nt: getCurrent().keySet()) {
                Production p = getCurrent().get(nt);

                if (didFirst) {
                    buffer.append(",");
                } else {
                    didFirst = true;
                }
                buffer.append(String.format("%s=%s", nt, p));
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
        return unclosed.hashCode();
    }

    /**
     * Two states are equal if their cost maps are equal.
     * @param o the object to compare against.
     * @return unclosed.equals(o.unclosed) if o is a State,
     * false otherwise.
     */
    @Override
    public boolean equals(Object o)
    {
        return o instanceof State?
            this.unclosed.equals(((State)o).unclosed):
            false;
    }
    
    class CostMap extends HashMap<Nonterminal, Production<Nonterminal,NodeType>>
    {
        CostMap()
        {
        }

        CostMap(CostMap src)
        {
            for (Nonterminal nt: src.keySet()) {
                this.put(nt, src.get(nt));
            }
        }

        long getCost(Nonterminal nt)
        {
            return this.containsKey(nt)? this.get(nt).ownCost: Integer.MAX_VALUE;
        }
    }
    
    class ClosureMap extends HashMap<Nonterminal, Closure<Nonterminal>>
    {
        long getCost(Nonterminal nt)
        {
            return this.containsKey(nt)? this.get(nt).ownCost: Integer.MAX_VALUE;
        }
    }
}
