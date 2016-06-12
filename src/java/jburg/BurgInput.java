package jburg;

/**
 * BurgInput declares the interface between the input tree
 * and the generated tree parser.
 * <h2>Callback API</h2>
 * <p>The pre and post reduction callback APIs are not specified
 * in the Java interface; their signatures are:
 * <li>Node.preCallback(Nonterminal goal)
 * <li>Object Node.postCallback(Object antecdedent)
 * for closures' post reduction callbacks; the antecedent
 * is the result of reducing the production that derives
 * the input node to the closure's source nonterminal.
 * <li>Object Node.postCallback(...)
 * for pattern matchers' callbacks. The arguments should
 * match the number of child nonterminals in the pattern in
 * number and type; a future release of the BURG will check.
 */
public interface BurgInput<Nonterminal, NodeType>
{
    /**
     * @return a node's node type.
     */
    public NodeType             getNodeType();

    /**
     * @return a nodes' number of subtrees.
     */
    public int                  getSubtreeCount();

    /**
     * @param idx   an index number in the range 0..getSubtreeCount().
     * @return the subtree at the specified index.
     */
    public BurgInput<Nonterminal, NodeType>  getSubtree(int idx);

    /**
     * Set a node's state number.
     * Note: the state number is normally, but not necessarily,
     * set only once; if a grammar has semantic predicates on
     * productions that could be used to derive the node, the
     * predicate resolution logic may reset the state number.
     */
    public void                 setStateNumber(int stateNumber);

    /**
     * Get a node's state number.
     * @return the last state number set by setStateNumber(),
     * or -1 if no state number has been set.
     */
    public int                  getStateNumber();

    /**
     * Get a node's transition table leaf.
     * @return the transition table leaf assigned to this node.
     */
    public Object getTransitionTableLeaf();

    /**
     * Set a node's transition table leaf.
     * @param ttLeaf    the applicable transition table leaf.
     */
    public void setTransitionTableLeaf(Object ttleaf);
}
