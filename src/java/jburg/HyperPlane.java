package jburg;

import java.util.*;

/**
 * A HyperPlane is a representation of one dimension
 * of the BURM's transition table: a multi-dimensional map
 * of child state tuples to the corresponding state to be
 * assigned to the subtree root.
 * Each Operator has a reference to the first dimension
 * of its transtition table; each subtable maps its
 * representer states to the next dimension, or in the
 * case of the final dimension, to the states which can
 * be assigned to the subtree root.
 */
class HyperPlane<Nonterminal, NodeType>
{
    /**
     * The next dimension of the table, if this is not the final dimension.
     */
    final List<HyperPlane<Nonterminal, NodeType>> nextDimension = new ArrayList<HyperPlane<Nonterminal, NodeType>>();

    /**
     * State number to transition table index for the next dimension.
     */
    final Map<Integer, Integer> nextDimIndexMap = new HashMap<Integer, Integer>();

    /**
     * The states in this dimension, if this is the final dimension.
     */
    final List<PredicatedState<Nonterminal, NodeType>> finalDimension = new ArrayList<PredicatedState<Nonterminal, NodeType>>();

    /**
     * State number to transition table index for the final dimension.
     */
    final Map<Integer, Integer> finalDimIndexMap = new HashMap<Integer, Integer>();

    /**
     * Is the given state number a valid state number for a variadic child?
     * @param stateNumber           the child's state number.
     * @param examineFinalDimension true if the child is the last child.
     * @return true if the state number is in the appropriate state-to-index map.
     */
    boolean isValidVariadicChild(int stateNumber, boolean examineFinalDimension)
    {
        return examineFinalDimension?
            finalDimIndexMap.containsKey(stateNumber):
            nextDimIndexMap.containsKey(stateNumber);
    }

    /**
     * Load a PredicatedState into the final dimension.
     * @param stateNum  the state's state number.
     * @param state     the state.
     */
    void loadPredicatedState(Integer sNum, PredicatedState<Nonterminal, NodeType> state)
    {
        int newStateIdx = finalDimension.size();
        finalDimension.add(state);
        finalDimIndexMap.put(sNum, newStateIdx);
    }

    /**
     * Load a child HyperPlane into the next dimension.
     * @param hyperPlane    the child hyperplane.
     * @param mappedStates  the states the hyperplane is mapped to.
     */
    void loadHyperPlane(HyperPlane<Nonterminal, NodeType> hyperPlane, Integer[] mappedStates)
    {
        assert mappedStates.length > 0: "empty mapped states";

        if (mappedStates.length > 0) {
            int hyperPlaneIndex = nextDimension.size();
            nextDimension.add(hyperPlane);

            for (Integer sNum: mappedStates) {
                nextDimIndexMap.put(sNum, hyperPlaneIndex);
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
     * @return the corresponding HyperPlane.
     */
    HyperPlane<Nonterminal, NodeType> getNextDimension(int stateNumber)
    {
        if (nextDimIndexMap.containsKey(stateNumber)) {
            assert nextDimIndexMap.get(stateNumber) < nextDimension.size(): String.format("state %d index %d out of range 0..%d",stateNumber, nextDimIndexMap.get(stateNumber), nextDimension.size());
            return nextDimension.get(nextDimIndexMap.get(stateNumber));
        } else {
            return null;
        }
    }

    /**
     * Assign a state number to a node.
     * @pre this must be the final dimension of the hyperplane.
     * @param stateNumber   the state number of the node's last child
     * (or the number of the null node production, if that child is null).
     * @param node      the node being labelled.
     * @param visitor   the receiver object for predicate method calls.
     * @post the node's state number will be assigned.
     * @throws Exception of arbitrary type from predicate invocation.
     */
    void assignStateNumber(int stateNumber, BurgInput<Nonterminal, NodeType> node, Object visitor)
    throws Exception
    {
        if (finalDimIndexMap.containsKey(stateNumber)) {
            State<Nonterminal, NodeType> result = finalDimension.get(finalDimIndexMap.get(stateNumber)).getState(node, visitor);
            node.setStateNumber(result.number);
            node.setTransitionTableLeaf(result);
        } else {
            node.setStateNumber(ProductionTable.ERROR_STATE_NUM);
        }
    }

    /**
     * Find all states mapped to a child hyperplane.
     * @param idx   the index of the child of interest.
     * @return a list of state numbers mapped to that hyperplane.
     */
    List<Integer> getStatesForPlane(int stateIndex)
    {
        List<Integer> result = new ArrayList<Integer>();

        for (Integer stateNum: nextDimIndexMap.keySet()) {

            if (nextDimIndexMap.get(stateNum) == stateIndex) {
                result.add(stateNum);
            }
        }

        return result;
    }

    /**
     * Does this transition table dimension contain the given state?
     * @param stateNumber   the state number of interest.
     * @return true if the state number is present in any state-to-index mapping.
     */
    boolean containsState(Integer stateNumber)
    {
        return nextDimIndexMap.containsKey(stateNumber) || finalDimIndexMap.containsKey(stateNumber);
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

}
