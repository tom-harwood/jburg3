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

    /**
     * If this State is a leaf state, there is either only one state
     * (if the productions had no semantic guards), or a state that
     * represents the "high" node in a lattice-like structure of states
     * that correspond to the results of the semantic guards.
     */
    State<Nonterminal, NodeType> leafState = null;

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
        RepresenterState<Nonterminal, NodeType> result = indexMap.get(dim).get(key);

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

    @Override
    public String toString()
    {
        return String.format("Operator %s[%d]", nodeType, size());
    }
}
