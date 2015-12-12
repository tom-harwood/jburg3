import java.util.ArrayList;
import java.util.List;

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

    /*
     * ** Nullary Operators **
     */
    public Integer intLiteral()
    {
        return Integer.valueOf(content);
    }

    public Short shortLiteral()
    {
        return Short.valueOf(content);
    }

    /*
     * ** Unary Operators **
     */
    public Integer negate(Integer x)
    {
        return -x;
    }

    public Integer identity(Integer x)
    {
        return x;
    }

    /*
     * ** Binary Operators **
     */
    public Integer add(Integer x, Integer y)
    {
        return x + y;
    }

    public Integer subtract(Integer x, Integer y)
    {
        return x - y;
    }

    public Integer multiply(Integer x, Integer y)
    {
        return x * y;
    }

    public Integer divide(Integer x, Integer y)
    {
        return x / y;
    }

    public String stringLiteral()
    {
        return content;
    }

    public String concat(String lhs, String rhs)
    {
        return lhs + rhs;
    }

    /**
     * ** Ternary operators, for testing **
     */
    public Integer addTernary(Integer x, Integer y, Integer z)
    {
        return x + y + z;
    }

    /*
     * ** Conversion operators **
     */
    public Integer widenShortToInt(Short operand)
    {
        return operand.intValue();
    }

    /*
     * ** N-ary operators
     */
    public Integer addNary(Integer... args)
    {
        int result = 0;
        for (Integer i: args) {
            result += i;
        }
        return result;
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
