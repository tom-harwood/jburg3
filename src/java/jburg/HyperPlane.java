package jburg;

import java.util.*;

/**
 * A HyperPlane is a representation of one dimension
 * of a multi-dimensional map of child state tuples
 * to the corresponding states of the subtree root.
 * Each Operator has a reference to the first dimension
 * of its multidimensional map; each dimension maps its
 * representer states to the next dimension, or in the
 * case of the final dimension, to the states which can
 * be assigned to the subtree root.
 * <p>A note on variadic pattern matchers: the final
 * dimension of a variadic matcher contains both mappings
 * (back to itself) for the next dimension and mappings
 * to the resultant states at the last element in the tuple.
 */
class HyperPlane<Nonterminal, NodeType>
{
    Map<HyperKey<Nonterminal, NodeType>, HyperPlane<Nonterminal, NodeType>> nextDimension;
    Map<HyperKey<Nonterminal, NodeType>, State<Nonterminal, NodeType>>      finalDimension;

    HyperPlane()
    {
        nextDimension   = new HashMap<HyperKey<Nonterminal, NodeType>, HyperPlane<Nonterminal, NodeType>>();
        finalDimension  = new HashMap<HyperKey<Nonterminal, NodeType>, State<Nonterminal, NodeType>>();
    }

    void addHyperPlane(Nonterminal needle, List<RepresenterState<Nonterminal, NodeType>> childStates, int currentDim, State<Nonterminal, NodeType> resultantState)
    {
        RepresenterState<Nonterminal, NodeType> currentRs = currentDim < childStates.size()?
            childStates.get(currentDim): null;

        HyperKey<Nonterminal, NodeType> key = new HyperKey<Nonterminal, NodeType>(needle, currentRs);

        if (currentDim < childStates.size() - 1) {

            if (!nextDimension.containsKey(key)) {
                nextDimension.put(key, new HyperPlane<Nonterminal, NodeType>());
            }
            nextDimension.get(key).addHyperPlane(needle, childStates, currentDim+1, resultantState);

        } else {
            finalDimension.put(key, resultantState);
        }
    }

    @Override
    public String toString()
    {
        if (nextDimension.isEmpty()) {
            return finalDimension.toString();
        } else {
            return String.format("HyperPlane{%s %s}", nextDimension, finalDimension);
        }
    }

    void dump(java.io.PrintWriter out)
    throws java.io.IOException
    {
        if (nextDimension.isEmpty()) {
            // We're at a leaf.
            for (HyperKey<Nonterminal, NodeType> key: finalDimension.keySet()) {
                State<Nonterminal, NodeType> goalState = finalDimension.get(key);

                if (key.rs == null) {
                    out.printf("<leaf state=\"%d\"/>\n", goalState.number);
                } else {
                    for (State<Nonterminal, NodeType> s: key.rs.representedStates) {
                        out.printf("<plane state=\"%d\"><leaf state=\"%d\"/></plane>\n", s.number, goalState.number);
                    }
                }
            }
        } else {
                
            for (HyperKey<Nonterminal, NodeType> key: nextDimension.keySet()) {
                assert key.rs != null;

                for (State s: key.rs.representedStates) {
                    out.printf("<plane state=\"%d\">", s.number);
                    nextDimension.get(key).dump(out);
                    out.println("</plane>");
                }
            }
        }
    }

    class HyperKey<Nonterminal, NodeType>
    {
        final Nonterminal nt;
        final RepresenterState<Nonterminal, NodeType>   rs;

        HyperKey(Nonterminal nt, RepresenterState<Nonterminal, NodeType> rs)
        {
            this.nt = nt;
            this.rs = rs;
        }

        @Override
        public int hashCode()
        {
            return rs != null? nt.hashCode() * 31 + rs.hashCode(): nt.hashCode();
        }

        @Override
        public boolean equals(Object x)
        {
            if (x instanceof HyperKey) {
                HyperKey k = (HyperKey) x;

                if (this.nt.equals(k.nt)) {

                    if (this.rs != null && k.rs != null) {
                        return this.rs.equals(k.rs);
                    } else {
                        return this.rs == k.rs;
                    }
                }
            }

            return false;
        }

        @Override
        public String toString()
        {
            return rs != null? String.format("HyperKey{%s,%s}", nt, rs): nt.toString();
        }
    }
}
