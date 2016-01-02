package jburg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An Operator represents an opcode(operand, operand) tuple.
 * The pattern-matching and closure productions for all inputs
 * with this opcode/arity are encoded into the operator's
 * transition table.
 */
class Operator<Nonterminal, NodeType>
{
    /**
     * The operator's node type, denormalized for debugging.
     */
    final NodeType nodeType;

    /**
     * The root hyperplane of the transition table.
     */
    HyperPlane<Nonterminal, NodeType> transitionTable;

    /**
     * Representer states known by each dimension; a representer
     * state only needs to be tried once in each dimension, so
     * remembering rep states speeds up the transition computations.
     */
    final List<Set<RepresenterState<Nonterminal,NodeType>>> reps;

    /**
     * Lookup table of RepresenterStates by state number;
     * there is a sub-table for each dimension.
     */
    final List<Map<Integer, RepresenterState<Nonterminal, NodeType>>> indexMap;

    /**
     * If this State is a leaf state, there is either only one state
     * (if the productions had no semantic guards), or a state that
     * represents the "high" node in a lattice-like structure of states
     * that correspond to the results of the semantic guards.
     */
    State<Nonterminal, NodeType> leafState = null;

    /**
     * This operator's arity kind: unknown, fixed-arity, or variadic.
     * The arity kind starts out unknown, and is set by the first state
     * added to the operator; subsequent productions must have the same
     * arity kind. The ProductionTable manages mixed fixed and variadic
     * productions; such productions generally entail creating copies of
     * states, and possibly copying or shifting operators.
     */
    ArityKind arityKind = null;

    /**
     * @param nodeType  the Operator's node type.
     * @param arity     the Operator's arity.
     * @todo variadic operators.
     */
    Operator(NodeType nodeType, int arity)
    {
        this.nodeType = nodeType;

        if (arity > 0) {
            this.transitionTable = new HyperPlane<Nonterminal, NodeType>();
            this.reps = new ArrayList<Set<RepresenterState<Nonterminal,NodeType>>>();

            for (int i = 0; i < arity; i++) {
                reps.add(new HashSet<RepresenterState<Nonterminal,NodeType>>());
            }

            this.indexMap = new ArrayList<Map<Integer, RepresenterState<Nonterminal, NodeType>>>();

            for (int i = 0; i < arity; i++) {
                indexMap.add(new HashMap<Integer, RepresenterState<Nonterminal, NodeType>>());
            }
        } else {
            reps = null;
            indexMap = null;
        }
    }

    /**
     * Get this Operator's size, i.e., its arity.
     * @return the operator's fixed arity.
     */
    int size()
    {
        return reps != null? reps.size(): 0;
    }

    /**
     * Get a leaf Operator's single state.
     * @todo leaf operators with semantic guards
     * will have multiple states.
     */
    State<Nonterminal, NodeType> getLeafState()
    {
        assert leafState != null;
        return leafState;
    }

    /**
     * Set the leaf state.
     * @pre the Operator must be of arity zero.
     */
    void setLeafState(State<Nonterminal, NodeType> leafState)
    {
        assert reps == null;
        assert this.leafState == null;
        this.leafState = leafState;
    }

    /**
     * Map a state to its corresponding representer state in the given dimension.
     * @param state the state to be mapped.
     * @param dim   the dimension to search.
     * @return      the RepresenterState that represents the state in that dimension.
     * @throws      IllegalArgumentException if the state has no mapping to a
     * representer state in the specified dimension.
     */
    RepresenterState<Nonterminal, NodeType> getRepresenterState(Integer key, int dim)
    {
        int nominalDim = isVarArgs()?
            dim < indexMap.size()? dim: indexMap.size() - 1:
            dim;

        RepresenterState<Nonterminal, NodeType> result = indexMap.get(nominalDim).get(key);

        if (result == null) {
            throw new IllegalArgumentException(String.format("State %d has no representer in dimension %d of %s", key, dim, this));
        }

        return result;
    }

    /**
     * Add an entry to this operator's transition table.
     * @param childStates       the list of representer states that produced
     * the transition; these representer states are its compound key.
     * @param resultantState    the state produced by this transition.
     */
    void addTransition(List<RepresenterState<Nonterminal,NodeType>> childStates, State<Nonterminal,NodeType> resultantState)
    {
        assert childStates.size() == this.size();
        assert resultantState.arityKind != null;

        if (arityKind == null) {
            arityKind = resultantState.arityKind;
        } else if (arityKind != resultantState.arityKind) {
            throw new IllegalStateException("Cannot mix variadic and fixed arity productions");
        }

        transitionTable.add(childStates, 0, resultantState);

        for (int dim = 0; dim < size(); dim++) {
            Map<Integer, RepresenterState<Nonterminal, NodeType>> indexForDim = indexMap.get(dim);
            RepresenterState<Nonterminal, NodeType> rs = childStates.get(dim);

            // Add the state to the operator's state->representer state
            // lookup table. It may already be present, because we create
            // a canonical state to represent all equivalent states, and
            // that state may have already interacted with this operator;
            // but in that case we must be adding the same representer state.
            for (State<Nonterminal, NodeType> s: rs.representedStates) {

                assert !indexForDim.containsKey(s.number) || indexForDim.get(s.number).equals(rs): String.format("Operator %s expected rs %s, got %s", this, indexForDim.get(s.number), rs);
                indexForDim.put(s.number, rs);
            }
        }
    }

    /**
     * Is this operator variadic?
     * @return true if all the operator's patterns are variadic.
     */
    boolean isVarArgs()
    {
        // Ensure the operator's ready.
        if (arityKind == null) {
            throw new IllegalStateException(String.format("Operator %s is incomplete; run generateStates() and ensure all child nonterminals are feasible",this));
        }

        return arityKind == ArityKind.Variadic;
    }

    /**
     * Dump this operator as XML.
     * @param out   the output sink.
     */
    void dump(java.io.PrintWriter out)
    throws java.io.IOException
    {
        out.printf("<operator nodeType=\"%s\" arity=\"%d\">\n", nodeType, size());
        if (transitionTable != null) {
            transitionTable.dump(out);
        } else if (leafState != null) {
            leafState.miniDump(out);
        }
        out.println("</operator>");
    }

    /**
     * Create an iterator over permutations of this operator's representer states,
     * with a novel representer state in one dimension.
     * @param pState    the novel representer state.
     * @param pStateDim the dimension of the novel state.
     * @return an iterator over permutations of this operator's rep states
     * and the novel state.
     */
    Iterable<List<RepresenterState<Nonterminal,NodeType>>> generatePermutations(RepresenterState<Nonterminal, NodeType> pState, int pStateDim)
    {
        return new RepresenterStatePermuationGenerator(pState, pStateDim);
    }

    @Override
    public String toString()
    {
        return String.format("Operator %s[%d]", nodeType, size());
    }

    /**
     * A RepresenterStatePermuationGenerator generates permutations of the parent
     * Operator's representer states, with a novel state provided by the caller
     * in one dimension.
     */
    class RepresenterStatePermuationGenerator implements Iterable<List<RepresenterState<Nonterminal,NodeType>>>
    {
        /**
         * Representer states to permuate. Except for the dimension where the
         * novel state is substituted, these are the states from the parent
         * operator's representer states for each dimension.
         */
        List<List<RepresenterState<Nonterminal,NodeType>>>  rsTable;

        /**
         * Current index in the representer state table for each dimension.
         * The index for dimensions 1..n ranges from 0..rsTable[dim].size()-1,
         * at which point it resets to 0 and "carries" to the next lowest
         * dimension. The index of dimension 0 ranges from 0..rsTable[0].size();
         * when dimension 0's index reaches rsTable[0].size() the set of permutations
         * has been exhausted.
         */
        int[]   rsIndex;

        /**
         * If this operator doesn't have rep sets for all dimensions yet,
         * don't try to generate permuations.
         */
        boolean hasRepSetsForEachDimension;

        RepresenterStatePermuationGenerator(RepresenterState<Nonterminal, NodeType> pState, int pStateDim)
        {
            int arity = Operator.this.size();

            rsTable = new ArrayList<List<RepresenterState<Nonterminal,NodeType>>>();
            rsIndex = new int[arity];

            // Assume we have representer states
            // for each dimension; we'll test that
            // hypothesis as we populate the lists.
            hasRepSetsForEachDimension = true;

            for (int i = 0; hasRepSetsForEachDimension && i < arity; i++) {
                rsIndex[i] = 0;
                rsTable.add(new ArrayList<RepresenterState<Nonterminal,NodeType>>());

                if (i == pStateDim) {
                    rsTable.get(i).add(pState);
                } else {
                    rsTable.get(i).addAll(reps.get(i));
                    hasRepSetsForEachDimension &= rsTable.get(i).size() > 0;
                }
            }
        }

        @Override
        public Iterator<List<RepresenterState<Nonterminal,NodeType>>> iterator()
        {

            return new Iterator<List<RepresenterState<Nonterminal,NodeType>>>()
            {
                @Override
                public boolean hasNext()
                {
                    assert rsIndex.length > 0: "Cannot permute a leaf operator";
                    return hasRepSetsForEachDimension && rsIndex[0] < rsTable.get(0).size();
                }

                @Override
                public List<RepresenterState<Nonterminal,NodeType>> next()
                {
                    List<RepresenterState<Nonterminal,NodeType>> result = new ArrayList<RepresenterState<Nonterminal,NodeType>>();

                    for (int i = 0; i < rsIndex.length; i++) {
                        result.add(rsTable.get(i).get(rsIndex[i]));
                    }

                    // Starting with the last dimension, increment indexes;
                    // this process continues until a dimension has more
                    // rep states to examine, or until we reach the first
                    // dimension, where we stop and the iterator is at end.
                    int idx = rsIndex.length - 1;
                    rsIndex[idx]++;

                    while (idx > 0 && rsIndex[idx] >= rsTable.get(idx).size()) {
                        rsIndex[idx] = 0;
                        rsIndex[idx-1]++;
                        idx--;
                    }

                    return result;
                }
            };
        }
    }
}
