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
    /**
     * The next dimension of the map, if this is not the final dimension.
     */
    final List<HyperPlane<Nonterminal, NodeType>> nextDimension = new ArrayList<HyperPlane<Nonterminal, NodeType>>();

    /**
     * The states in this dimension, if this is the final dimension.
     */
    final List<PredicatedState<Nonterminal, NodeType>> finalDimension = new ArrayList<PredicatedState<Nonterminal, NodeType>>();

    /**
     * The transition table proper: index numbers of the next dimension
     * to match against the next state, by state number.
     */
    final Map<Integer, Integer> nextDimIndexMap = new HashMap<Integer, Integer>();

    /**
     * The transition table proper: index numbers of the predicated
     * state to use, keyed by state number.
     */
    final Map<Integer, Integer> finalDimIndexMap = new HashMap<Integer, Integer>();

    /**
     * Representer states' mappings to indexes in the nextDimension;
     * used to add new states that already have representer states to the mapping.
     */
    final RepresenterStateToIndexMap    nextDimRsMap = new RepresenterStateToIndexMap();

    /**
     * Representer states' mappings to indexes in the finalDimension;
     * used to add new states that already have representer states to the mapping.
     */
    final RepresenterStateToIndexMap    finalDimRsMap = new RepresenterStateToIndexMap();

    /**
     * Construct a HyperPlane.
     */
    HyperPlane()
    {
    }

    /**
     * Add a transition to this transition subtable. This may involve extending existing
     * subtrees, if they already have mappings for any of the states in the new state's
     * representer states, or it may be a straightforward addition.
     * @param childStates       the list of representer states that identify this transition.
     * @param currentDim        this subtable's dimension in the transition table.
     * @param resultantState    the composite state at the vertex of this transition.
     */
    void addTransition(List<RepresenterState<Nonterminal, NodeType>> childStates, int currentDim, PredicatedState<Nonterminal, NodeType> resultantState)
    {

        assert childStates.size() > 0;

        RepresenterState<Nonterminal, NodeType> rs = childStates.get(currentDim);

        Set<Integer> processedChildPlanes = new HashSet<Integer>();
        Set<Integer> novelStateNumbers    = new HashSet<Integer>();

        // First, find all the states that already have mappings in this prefix.
        for (State<Nonterminal, NodeType> s: rs.representedStates) {

            // This should not happen; it implies that two disjoint vectors
            // of representer states, which are known to have at least one
            // disjoint member, can produce the same state.
            if (finalDimIndexMap.containsKey(s.number)) {

                if (!nextDimIndexMap.containsKey(s.number)) {
                    throw new IllegalStateException(String.format("state %s has divergent representation %d in dimension %d of the transition table", s.number, finalDimIndexMap.get(s.number), currentDim));
                } else {
                    assert nextDimension.get(nextDimIndexMap.get(s.number)) == this: "divergent final state mappings";
                }
            }

            if (nextDimIndexMap.containsKey(s.number)) {
                Integer idx = nextDimIndexMap.get(s.number);

                if (processedChildPlanes.add(idx)) {
                    HyperPlane<Nonterminal, NodeType> child = nextDimension.get(idx);

                    if (child != this) {
                        child.addTransition(childStates, currentDim+1, resultantState);
                    }
                }
            } else {
                novelStateNumbers.add(s.number);
            }
        }

        if (currentDim < childStates.size() - 1) {
            if (novelStateNumbers.size() > 0) {
                addHyperPlane(childStates, novelStateNumbers, currentDim+1, resultantState);
            }
        } else {
            int newStateNum = finalDimension.size();
            finalDimension.add(resultantState);
            finalDimRsMap.addMapping(rs, newStateNum);

            // Any of the represented states might have
            // already been mapped to a resultant state;
            // the new resultant state will have the same
            // or better costs, so use it unconditionally.
            for (Integer sNum: novelStateNumbers) {
                finalDimIndexMap.put(sNum, newStateNum);
            }

            // If all the states in this final dimension are variadic,
            // add a variadic transition back to this hyperplane.
            // Note that the current state of the ProductionTable's
            // variadic handling dictates that if any state is
            // variadic, then they all are.
            if (isVarArgs()) {
                int thisNum = nextDimension.indexOf(this);

                if (thisNum == -1) {
                    thisNum = nextDimension.size();
                    nextDimension.add(this);
                }

                nextDimRsMap.addMapping(rs, thisNum);

                for (Integer sNum: novelStateNumbers) {
                    nextDimIndexMap.put(sNum, thisNum);
                }
            }
        }
    }

    private HyperPlane getNextDimension(Integer stateNumber)
    {
        assert nextDimIndexMap.containsKey(stateNumber);
        return nextDimension.get(nextDimIndexMap.get(stateNumber));
    }

    private void addHyperPlane(List<RepresenterState<Nonterminal, NodeType>> childStates, Set<Integer> novelStateNumbers, int nextDim, PredicatedState<Nonterminal, NodeType> resultantState)
    {
        Integer newHyperPlaneIdx = nextDimension.size();
        HyperPlane<Nonterminal, NodeType> newHyperPlane = new HyperPlane<Nonterminal, NodeType>();
        nextDimension.add(newHyperPlane);
        newHyperPlane.addTransition(childStates, nextDim, resultantState);
        nextDimRsMap.addMapping(childStates.get(nextDim-1), newHyperPlaneIdx);

        for (Integer sNum: novelStateNumbers) {
            nextDimIndexMap.put(sNum, newHyperPlaneIdx);
        }
    }

    void addRepresentedState(State<Nonterminal, NodeType> s, int dim, RepresenterState<Nonterminal, NodeType> pState)
    {
        if (dim == 0) {

            for (Integer index: nextDimRsMap.getMappings(pState)) {
                nextDimIndexMap.put(s.number, index);
            }

            for (Integer index: finalDimRsMap.getMappings(pState)) {
                nextDimIndexMap.put(s.number, index);
            }

        } else {
            for (HyperPlane<Nonterminal, NodeType> child: nextDimension) {
                child.addRepresentedState(s, dim-1, pState);
            }
        }
    }

    /**
     * Is this HyperPlane variadic?
     * @return true if all this HyperPlane's productions,
     * as well as all its descendents' productions, are variadic.
     */
    boolean isVarArgs()
    {
        for (PredicatedState<Nonterminal, NodeType> s: finalDimension) {

            if (!s.isVarArgs()) {
                return false;
            }
        }

        for (HyperPlane<Nonterminal, NodeType> child: nextDimension) {
            if (!(child == this || child.isVarArgs())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get the next dimension of this hyperplane.
     * @param rs the RepresenterState in the next dimension.
     * @return the corresponding hyperplane.
     * @throws IllegalStateException if there is no corresponding hyperplane.
     */
    HyperPlane<Nonterminal, NodeType> getNextDimension(int stateNumber)
    {
        if (nextDimIndexMap.containsKey(stateNumber)) {
            assert nextDimIndexMap.get(stateNumber) < nextDimension.size(): String.format("state %d index %d out of range 0..%d",stateNumber, nextDimIndexMap.get(stateNumber), nextDimension.size());
            return nextDimension.get(nextDimIndexMap.get(stateNumber));
        } else {
            // TODO: return null, let the operator sort it out.
            throw new IllegalStateException(String.format("No hyperplane mapping for state %d", stateNumber));
        }
    }

    /**
     * Assign a state number to a node.
     * @pre this must be the final dimension of the hyperplane.
     * @param node      the node being labelled.
     * @param visitor   the receiver object for predicate method calls.
     * @post the node's state number will be assigned.
     * @throws Exception of arbitrary type from predicate invocation.
     */
    void assignStateNumber(int stateNumber, BurgInput<NodeType> node, Object visitor)
    throws Exception
    {
        assert !finalDimension.isEmpty();
        if (finalDimIndexMap.containsKey(stateNumber)) {
            State<Nonterminal, NodeType> result = finalDimension.get(finalDimIndexMap.get(stateNumber)).getState(node, visitor);
            node.setStateNumber(result.number);
        } else {
            // TODO: Assign the error state
            throw new IllegalStateException(String.format("No hyperplane mapping for state %d", stateNumber));
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

    /**
     * Dump this hyperplane.
     * @param out   the dump sink.
     */
    void dump(java.io.PrintWriter out)
    throws java.io.IOException
    {
        /*
        if (nextDimension.isEmpty()) {

            for (RepresenterState<Nonterminal, NodeType> key: finalDimension.keySet()) {

                for (State<Nonterminal, NodeType> s: key.representedStates) {
                    s.miniDump(out);
                }
            }
        } else {
                
            for (RepresenterState<Nonterminal, NodeType> key: nextDimension.keySet()) {

                for (State<Nonterminal, NodeType> s: key.representedStates) {
                    out.printf("<plane state=\"%d\">", s.number);
                    if (nextDimension.get(key) != this) {
                        nextDimension.get(key).dump(out);
                    } else {
                        out.println("<variadic/>");
                    }
                    out.println("</plane>");
                }
            }
        }
        */
    }

    private class RepresenterStateToIndexMap
    {
        private Map<RepresenterState<Nonterminal, NodeType>, Set<Integer>> mappings = new HashMap<RepresenterState<Nonterminal, NodeType>, Set<Integer>>();

        void addMapping(RepresenterState<Nonterminal, NodeType> rs, Integer index)
        {
            if (!mappings.containsKey(rs)) {
                mappings.put(rs, new HashSet<Integer>());
            }

            mappings.get(rs).add(index);
        }

        Iterable<Integer> getMappings(RepresenterState<Nonterminal, NodeType> rs)
        {
            return mappings.containsKey(rs)?
                mappings.get(rs):
                Collections.emptyList();
        }
    }
}
