package jburg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    Operator(NodeType nodeType, int arity)
    {
        this.nodeType = nodeType;
        this.transitionTable = new HyperPlane<Nonterminal, NodeType>();
        this.reps = new ArrayList<Set<RepresenterState<Nonterminal,NodeType>>>();

        for (int i = 0; i < arity; i++) {
            reps.add(new HashSet<RepresenterState<Nonterminal,NodeType>>());
        }

        this.indexMap = new ArrayList<Map<Integer, RepresenterState<Nonterminal, NodeType>>>();

        for (int i = 0; i < arity; i++) {
            indexMap.add(new HashMap<Integer, RepresenterState<Nonterminal, NodeType>>());
        }
    }

    int size()
    {
        return reps.size();
    }

    State<Nonterminal, NodeType> getLeafState()
    {
        assert transitionTable.leafState != null;
        return transitionTable.leafState;
    }

    RepresenterState<Nonterminal, NodeType> getRepresenterState(Integer key, int dim)
    {
        RepresenterState<Nonterminal, NodeType> result = indexMap.get(dim).get(key);

        if (result == null) {
            throw new IllegalArgumentException(String.format("State %d has no representer in dimension %d of %s", key, dim, this));
        }

        return result;
    }

    void addTransition(List<RepresenterState<Nonterminal,NodeType>> childStates, State<Nonterminal,NodeType> resultantState)
    {
        assert childStates.size() == this.size();
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

                assert !indexForDim.containsKey(s.number) || indexForDim.get(s.number).equals(rs);
                indexForDim.put(s.number, rs);
            }
        }
    }

    @Override
    public String toString()
    {
        return String.format("Operator %s[%d]", nodeType, size());
    }
}
