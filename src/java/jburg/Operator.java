package jburg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An Operator represents an opcode(operand...) tuple.
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
     * If this State is a leaf state, it has a composite
     * state, with permutations of the leaf productions'
     * predicate methods (if any).
     */
    PredicatedState<Nonterminal, NodeType> leafState = null;

    /**
     * This operator's arity kind: unknown, fixed-arity, or variadic.
     * The arity kind starts out unknown, and is set by the first state
     * added to the operator; subsequent productions must have the same
     * arity kind. The ProductionTable manages mixed fixed and variadic
     * productions; combinations of fixed-arity and variadic productions
     * generally entail creating copies of states, and possibly copying
     * or shifting operators.
     */
    ArityKind arityKind = null;

    /**
     * The production table that created this operator; used to look up
     * the null pointer state, etc.
     */
    final ProductionTable<Nonterminal, NodeType> productionTable;

    /**
     * @param nodeType  the Operator's node type.
     * @param arity     the Operator's arity.
     */
    Operator(NodeType nodeType, int arity, ProductionTable<Nonterminal, NodeType> productionTable)
    {
        this.nodeType = nodeType;
        this.productionTable = productionTable;

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
     * Set a leaf Operator's state number into a node.
     * @param node      the node.
     * @param visitor   the semantic predicate receiver.
     */
    void setLeafState(BurgInput<NodeType> node, Object visitor)
    throws Exception
    {
        assert leafState != null;
        node.setStateNumber(leafState.getState(node, visitor).number);
    }

    /**
     * Create the leaf state.
     * @param states the set of leaf states.
     * @pre the Operator must be of arity zero.
     */
    void createLeafState(List<State<Nonterminal, NodeType>> states)
    {
        assert this.reps == null;
        assert this.leafState == null;
        this.leafState = new PredicatedState<Nonterminal, NodeType>(states);
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
            throw new IllegalArgumentException(String.format("State %d is not usable in dimension %d of %s", key, dim, this));
        }

        return result;
    }

    /**
     * Add an entry to this operator's transition table.
     * @param childStates       the list of representer states that produced
     * the transition; these representer states are its compound key.
     * state in the compound key; states may already have mappings in the
     * transition table, which will be extended.
     * @param resultantState    the state produced by this transition.
     */
    void addTransition(List<RepresenterState<Nonterminal,NodeType>> childStates, PredicatedState<Nonterminal,NodeType> resultantState)
    {
        assert childStates.size() == this.size();
        ArityKind stateArityKind = resultantState.getArityKind();

        if (this.arityKind == null) {
            this.arityKind = stateArityKind;
        } else if (this.arityKind != stateArityKind) {
            throw new IllegalArgumentException("Cannot mix variadic and fixed arity productions");
        }

        transitionTable.addTransition(childStates, 0, resultantState);
    }

    /**
     * We have a novel state that already has a representer state in this transition table;
     * add a mapping so we can find that representer state.
     * @param s         the novel state.
     * @param dim       the dimension of the transition table affected.
     * @param pState    a copy of the relevant representer state.
     */
    void addRepresentedState(State<Nonterminal, NodeType> s, int dim, RepresenterState<Nonterminal, NodeType> pState)
    {
        transitionTable.addRepresentedState(s, dim, pState);
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
            leafState.dump(out);
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

                    // Starting with the last dimension, increment indexes
                    // with carry; this process continues until a dimension
                    // has more rep states to examine, or until we reach the
                    // first dimension, where we stop and the iterator is at end.
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

    void assignState(BurgInput<NodeType> node, Object visitor)
    throws Exception
    {
        int subtreeCount = node.getSubtreeCount();
        HyperPlane<Nonterminal, NodeType> current = this.transitionTable;

        for (int dim = 0; current != null && dim < node.getSubtreeCount(); dim++) {
            BurgInput<NodeType> subtree = node.getSubtree(dim);
            int stateNumber = subtree != null? subtree.getStateNumber(): productionTable.getNullPointerState().number;

            if (stateNumber == -1) {
                current = null;

            } else if (dim < subtreeCount-1) {
                current = current.getNextDimension(stateNumber);

            } else {
                current.assignStateNumber(stateNumber, node, visitor);
            }
        }

        if (current == null) {
            node.setStateNumber(0);
        }
    }
}
