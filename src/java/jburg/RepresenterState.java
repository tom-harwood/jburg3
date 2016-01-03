package jburg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A RepresenterState is a "skeleton" representation
 * of a State, a tuple of (NodeType, {Nonterminal=cost}*).
 * Many states may map to the same RepresenterState.
 * The transition tables are keyed by tuples of RepresenterState
 * objects, which effects a very significant compaction of the
 * resulting transition table.
 * Computing representer states also speeds up the transition
 * table computation because the algorithm that creates them
 * only keeps nonterminals that have viable productions in
 * each dimension of the {Nonterminal=cost} map, so we avoid
 * useless tuples such as Add(String, Int) where there is a
 * production for Add(Int, Int) but none for Add(String,String).
 *
 * <p>An Operator's transition table is keyed by lists of
 * representer states; the state transition computation
 * maintains a map of state and corresponding representer
 * state in each dimension of a particular operator.
 */
class RepresenterState<Nonterminal,NodeType>
{
    /**
     * This representer state's node type. A representer
     * state is uniquely identified by its (nodeType, costMap)
     * tuple; this allows us to compute transitions for, e.g.,
     * the representer state (Add, int=1) and (Subtract, int=1)
     * but skips unproductive recomputation of the transitions
     * for these representer states once they've been tried.
     */
    private final NodeType              nodeType;

    /**
     * The cost to produce this representer state's nonterminals.
     */
    Map<Nonterminal, Long>              costMap = new HashMap<Nonterminal, Long>();
    Set<State<Nonterminal, NodeType>>   representedStates = new HashSet<State<Nonterminal, NodeType>>();

    /**
     * Create a RepresenterState.
     * @param nodeType  the node type of the state that
     * is being projected into this representer state.
     */
    RepresenterState(NodeType nodeType)
    {
        this.nodeType = nodeType;
    }

    /**
     * Set the cost to produce a nonterminal.
     * @param nt    the nonterminal.
     * @param cost  the cost.
     * @pre         the cost must be the best
     * cost seen so far.
     */
    void setCost(Nonterminal nt, long cost)
    {
        assert(cost < getCost(nt));
        costMap.put(nt,cost);
    }

    /**
     * Get the cost to produce a nonterminal.
     * @param nt    the nonterminal of interest.
     * @return      the cost to produce the nonterminal,
     * or Integer.MAX_VALUE if this representer state
     * has not cost associated with the specified nonterminal.
     */
    long getCost(Nonterminal nt)
    {
        return costMap.containsKey(nt)? costMap.get(nt): Integer.MAX_VALUE;
    }

    /**
     * @return true iff this representer state
     * has no entries in its cost map.
     */
    boolean isEmpty()
    {
        return costMap.isEmpty();
    }

    @Override
    /**
     * A representer state's hash code is a composite of
     * its node type and cost map's hash codes.
     */
    public int hashCode()
    {
        if (nodeType != null) {
            return nodeType.hashCode() * 31 + costMap.hashCode();
        } else {
            return costMap.hashCode();
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    /**
     * A representer state is equal to another representer state
     * if their node types and cost maps are equal.
     */
    public boolean equals(Object x)
    {
        if (x instanceof RepresenterState) {
            RepresenterState<Nonterminal,NodeType> rs = (RepresenterState<Nonterminal,NodeType>)x;

            if (this.nodeType != null) {
                return this.nodeType.equals(rs.nodeType) && this.costMap.equals(rs.costMap);
            } else {
                return rs.nodeType == null && this.costMap.equals(rs.costMap);
            }
        } else {
            return false;
        }
    }

    public Set<Nonterminal> keySet()
    {
        return costMap.keySet();
    }

    @Override
    public String toString()
    {
        return String.format("%s %s", nodeType, costMap);
    }
}
