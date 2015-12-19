import java.util.ArrayList;
import java.util.List;

/**
 * Node is a simple, dumb tree node.
 */
public class Node implements jburg.BurgInput<NodeType>
{
    NodeType    nodeType;
    List<Node>  children;
    String      content;

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

    @Override
    public String toString()
    {
        if (content == null) {
            return String.format("%s{%d}%s", nodeType, stateNumber, children);
        } else {
            return String.format("%s{%d}(%s)%s", nodeType, stateNumber, content, children);
        }
    }
}
