import java.util.ArrayList;
import java.util.List;
import jburg.State;

/**
 * Node is a simple tree node.
 */
public class Node implements jburg.BurgInput<Nonterminal, NodeType>
{
    NodeType    nodeType;
    List<Node>  children;
    String      content;

    /** State stored on behalf of the BURM. */
    State<Nonterminal, NodeType> transitionTableLeaf;

    /** State stored on behalf of the BURM. */
    private int stateNumber = -1;

    Node(NodeType type)
    {
        this.nodeType = type;
        this.children = new ArrayList<Node>();
    }

    Node(NodeType type, String content)
    {
        this(type);
        this.content = content;
    }

    void addChild(Node n)
    {
        this.children.add(n);
    }

    public NodeType getNodeType()
    {
        return this.nodeType;
    }

    public int getSubtreeCount()
    {
        return children.size();
    }

    public Node getSubtree(int idx)
    {
        return children.get(idx);
    }

    public void setStateNumber(int stateNumber)
    {
        this.stateNumber = stateNumber;
    }

    public int getStateNumber()
    {
        return this.stateNumber;
    }

    public State<Nonterminal, NodeType> getTransitionTableLeaf()
    {
        return this.transitionTableLeaf;
    }

    public void setTransitionTableLeaf(State<Nonterminal, NodeType> transitionTableLeaf)
    {
        this.transitionTableLeaf = transitionTableLeaf;
    }

    @Override
    public String toString()
    {
        return String.format("%s{%d}%s%s", nodeType, stateNumber, optional(content, "(%s)"), optional(children, " %s"));
    }

    private String optional(Object member, String format)
    {
        return member != null? String.format(format, member.toString()):"";
    }


}
