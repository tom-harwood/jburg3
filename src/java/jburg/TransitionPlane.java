package jburg;

import java.util.*;

/**
 * A TransitionPlane is a representation of one dimension
 * of the BURM's transition table: a multi-dimensional map
 * of child state tuples to the corresponding state to be
 * assigned to the subtree root.
 * Each Operator has a reference to the first dimension
 * of its transtition table; each subtable maps its
 * representer states to the next dimension, or in the
 * case of the final dimension, to the states which can
 * be assigned to the subtree root.
 */
public class TransitionPlane<Nonterminal, NodeType>
{
    /**
     * The next dimension of the table, if this is not the final dimension.
     */
    final List<TransitionPlane<Nonterminal, NodeType>> nextDimension;
    public List<TransitionPlane<Nonterminal, NodeType>> getNextDimension() { return nextDimension; }

    /**
     * State number to transition table index for the next dimension.
     */
    final Map<Integer, Integer> nextDimIndexMap;
    public Map<Integer, Integer> getNextDimIndexMap() { return nextDimIndexMap; }

    /**
     * The states in this dimension, if this is the final dimension.
     */
    final List<TransitionTableLeaf<Nonterminal, NodeType>> finalDimension;
    public List<TransitionTableLeaf<Nonterminal, NodeType>> getFinalDimension() { return finalDimension; }

    /**
     * State number to transition table index for the final dimension.
     */
    final Map<Integer, Integer> finalDimIndexMap;
    public Map<Integer, Integer> getFinalDimIndexMap() { return finalDimIndexMap; }

    /**
     * The dimension this plane occupies in the transition table;
     * denormalized data for use by emitters.
     */
    final int dimension;
    public int getDimension() { return dimension; }

    /**
     * Construct the final dimension of a transition.
     */
    TransitionPlane(Map<Integer,Integer> finalDimIndexMap, TransitionTableLeaf<Nonterminal,NodeType>[] finalDimension, int dim)
    {
        this.finalDimIndexMap = finalDimIndexMap;
        this.finalDimension = Arrays.asList(finalDimension);
        this.nextDimIndexMap = null;
        this.nextDimension = null;
        this.dimension = dim;
    }

    /**
     * Construct the next dimension of a transition.
     */
    TransitionPlane(Map<Integer,Integer> nextDimIndexMap, TransitionPlane<Nonterminal,NodeType>[] nextDimension, int dim)
    {
        this.nextDimIndexMap = nextDimIndexMap;
        this.nextDimension = Arrays.asList(nextDimension);
        this.finalDimIndexMap = null;
        this.finalDimension = null;
        this.dimension = dim;
    }

    /**
     * Construct a TransitionPlane whose characteristics
     * are not yet fully known (compiler compile time).
     */
    TransitionPlane(int dim)
    {
        this.nextDimIndexMap = new HashMap<Integer,Integer>();
        this.nextDimension = new ArrayList<TransitionPlane<Nonterminal,NodeType>>();
        this.finalDimIndexMap = new HashMap<Integer,Integer>();
        this.finalDimension = new ArrayList<TransitionTableLeaf<Nonterminal,NodeType>>();
        this.dimension = dim;
    }

    /**
     * Is the given state number a valid state number for a variadic child?
     * @param stateNumber           the child's state number.
     * @param examineFinalDimension true if the child is the last child.
     * @return true if the state number is in the appropriate state-to-index map.
     */
    boolean isValidVariadicChild(int stateNumber)
    {
        assert finalDimIndexMap != null: "no final dimension";
        return finalDimIndexMap.containsKey(stateNumber);
    }

    /**
     * Is this TransitionPlane variadic?
     * @return true if all this TransitionPlane's productions,
     * as well as all its descendents' productions, are variadic.
     */
    boolean isVarArgs()
    {
        for (TransitionTableLeaf<Nonterminal, NodeType> s: finalDimension) {

            if (!s.isVarArgs()) {
                return false;
            }
        }

        for (TransitionPlane<Nonterminal, NodeType> child: nextDimension) {
            if (!(child == this || child.isVarArgs())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get the next dimension of this hyperplane.
     * @param stateNumber the state number of the child in this plane's dimension.
     * @return the corresponding TransitionPlane.
     */
    TransitionPlane<Nonterminal, NodeType> getNextDimension(int stateNumber)
    {
        assert nextDimIndexMap != null: "No next dimension";

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

    Map<Integer,TransitionTableLeaf<Nonterminal,NodeType>> statesByIndex = null;

    public Map<Integer,TransitionTableLeaf<Nonterminal,NodeType>> getStatesByIndex()
    {
        if (statesByIndex == null) {
            statesByIndex = new HashMap<Integer,TransitionTableLeaf<Nonterminal,NodeType>>();

            for (Integer idx: finalDimIndexMap.keySet()) {
                statesByIndex.put(idx, finalDimension.get(finalDimIndexMap.get(idx)));
            }
        }

        return statesByIndex;
    }

    Map<Integer, TransitionPlane<Nonterminal,NodeType>> nextDimensionByIndex;
    public Map<Integer, TransitionPlane<Nonterminal,NodeType>> getNextDimensionByIndex()
    {
        if (nextDimensionByIndex == null) {
            nextDimensionByIndex = new HashMap<Integer, TransitionPlane<Nonterminal,NodeType>>();

            for (Integer idx: nextDimIndexMap.keySet()) {
                nextDimensionByIndex.put(idx, getNextDimension(idx));
            }
        }

        return nextDimensionByIndex;
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
            return String.format("TransitionPlane{%s %s}", nextDimension, finalDimension);
        }
    }

}
