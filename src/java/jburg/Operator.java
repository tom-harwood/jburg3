package jburg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * An Operator represents an opcode(operand...) tuple.
 * The pattern-matching and closure productions for all inputs
 * with this opcode/arity are encoded into the operator's
 * transition table.
 */
public class Operator<Nonterminal, NodeType>
{
    /**
     * The operator's node type.
     */
    public final NodeType nodeType;

    /**
     * The root hyperplane of the transition table.
     */
    TransitionPlane<Nonterminal, NodeType> transitionTable;

    public TransitionPlane<Nonterminal, NodeType> getTransitionTable()
    {
        return this.transitionTable;
    }

    /**
     * Representer states known by each dimension; used to
     * create permutations of representer states.
     */
    List<Set<RepresenterState<Nonterminal,NodeType>>> reps;

    /**
     * If this State is a leaf state, it has a composite
     * state, with permutations of the leaf productions'
     * predicate methods (if any).
     */
    TransitionTableLeaf<Nonterminal, NodeType> leafState = null;

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
    ProductionTable<Nonterminal, NodeType> productionTable = null;

    /**
     * The compiler compile-time representation of the productions
     * that will create the production table.
     */
    TransitionTableBuilder<Nonterminal, NodeType> builder;

    /**
     * The operator's minumum number of children.
     */
    final int arity;

    /**
     * @param nodeType  the Operator's node type.
     * @param arity     the Operator's arity.
     */
    Operator(NodeType nodeType, int arity, ProductionTable<Nonterminal, NodeType> productionTable)
    {
        this.nodeType = nodeType;
        this.productionTable = productionTable;
        this.arity = arity;
        this.builder = new TransitionTableBuilder<Nonterminal, NodeType>(productionTable, arity);

        if (arity > 0) {
            this.reps = new ArrayList<Set<RepresenterState<Nonterminal,NodeType>>>();

            for (int i = 0; i < arity; i++) {
                reps.add(new HashSet<RepresenterState<Nonterminal,NodeType>>());
            }

        } else {
            reps = null;
        }
    }

    /**
     * Get this Operator's size, i.e., its arity.
     * @return the operator's fixed arity.
     */
    int size()
    {
        return arity;
    }

    public int getSize()
    {
        return size();
    }

    /**
     * Build the transition table, and flush compiler-compile-time data structures.
     */
    void finishCompilation()
    {
        assert builder != null: String.format("Operator %s has already flushed compiler compile time data");
        if (!builder.isEmpty()) {
            this.transitionTable = builder.buildTransitionTable();
        }

        if (this.transitionTable == null && this.leafState == null) {
            throw new IllegalStateException(String.format("Operator %s is not a leaf or non-leaf -- probably because there is a child nonterminal with no productions.", this));
        }

        builder = null;
        reps = null;
    }

    /**
     * Set a leaf Operator's state number into a node.
     * @param node      the node.
     * @param visitor   the semantic predicate receiver.
     */
    void setLeafState(BurgInput<Nonterminal, NodeType> node, Object visitor)
    throws Exception
    {
        assert leafState != null;
        State<Nonterminal, NodeType> result = leafState.getState(node, visitor);
        node.setStateNumber(result.number);
        node.setTransitionTableLeaf(result);
    }

    /**
     * Create the leaf state.
     * @param states the set of leaf states.
     * @pre the Operator must be of arity zero.
     */
    void createLeafState(List<State<Nonterminal, NodeType>> states)
    {
        setLeafState(new TransitionTableLeaf<Nonterminal, NodeType>(states));
    }

    /**
     * Set the leaf state.
     * @param leafState the leaf state, typically from a load operation.
     */
    void setLeafState(TransitionTableLeaf<Nonterminal, NodeType> leafState)
    {
        assert this.reps == null;
        assert this.leafState == null;
        this.leafState = leafState;
        this.arityKind = ArityKind.Fixed;
    }

    /**
     * @return this operator's leaf state, or null if not present.
     */
    public TransitionTableLeaf<Nonterminal,NodeType> getLeafState()
    {
        return this.leafState;
    }

    /**
     * Add an entry to this operator's transition table.
     * @param repStates the representer states that produced the
     * transition; these representer states are its compound key.
     * @param state     the state produced by this transition.
     */
    void addTransition(List<RepresenterState<Nonterminal,NodeType>> repStates, State<Nonterminal,NodeType> state)
    {
        assert builder != null && repStates != null: "Compiler compile-time already completed";
        assert repStates.size() == this.size();
        ArityKind stateArityKind = state.arityKind;

        if (this.arityKind == null) {
            this.arityKind = stateArityKind;
        } else if (this.arityKind != stateArityKind) {
            // TODO: Prove this is sound.
            this.arityKind = ArityKind.Variadic;
        }

        builder.addTransition(repStates, state);
    }

    /**
     * Is this operator variadic?
     * @return true if the operator's patterns are variadic.
     */
    boolean isVarArgs()
    {
        return arityKind != null && arityKind == ArityKind.Variadic;
    }

    public boolean getVariadic()
    {
        return isVarArgs();
    }

    /**
     * Set this operator's arity kind while loading from a dump.
     */
    void setArityKind(ArityKind arityKind)
    {
        this.arityKind = arityKind;
    }

    public ArityKind getArityKind()
    {
        return this.arityKind;
    }

    /**
     * @return true if this Operator is ready
     * for use by a Reducer.
     */
    boolean isComplete()
    {
        return arityKind != null;
    }

    /**
     * Create a permutation generator of this operator's representer
     * states, with a novel representer state in one dimension.
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

    void assignState(BurgInput<Nonterminal, NodeType> node, Object visitor)
    throws Exception
    {
        int subtreeCount = node.getSubtreeCount();

        // Start at the root of the transition table, which is keyed
        // by the input node's subtrees' state numbers; each subtree
        // has a corresponding dimension in the transition table.
        // If no match then label the input node as an error.
        TransitionPlane<Nonterminal, NodeType> current = this.transitionTable;

        for (int dim = 0; dim < subtreeCount; dim++) {
            BurgInput<Nonterminal, NodeType> subtree = node.getSubtree(dim);
            int stateNumber = subtree != null? subtree.getStateNumber(): productionTable.getNullPointerState().number;

            if (current != null) {
                boolean isLastChild = dim == subtreeCount - 1;
                boolean isVariadicTail = isVarArgs() && dim >= arity - 1;

                if (!isLastChild) {

                    // If we're processing a variadic operator's
                    // variadic tail, continue "traversing" the
                    // current dimension; but check to ensure that
                    // the child is a valid input.
                    if (isVariadicTail) {

                        if (current.isValidVariadicChild(stateNumber)) {
                            continue;
                        } else {
                            node.setStateNumber(ProductionTable.ERROR_STATE_NUM);
                            node.setTransitionTableLeaf(productionTable.errorState);
                            break;
                        }
                    } else {
                        // A non-variadic child; traverse this dimension
                        // of the transition table.
                        current = current.getNextDimension(stateNumber);
                    }
                } else {
                    current.assignStateNumber(stateNumber, node, visitor);
                }
            } else {
                node.setStateNumber(ProductionTable.ERROR_STATE_NUM);
                node.setTransitionTableLeaf(productionTable.errorState);
                break;
            }
        }
    }
}
