package tql;

import java.util.List;
import jburg.BurgInput;

@SuppressWarnings("unchecked")
public class Node <Nonterminal,NodeType> implements BurgInput<Nonterminal,NodeType>
{
    final NodeType      type;
    final List<Node>    children;
    final Object        content;
    Object              transition;
    int                 stateNumber = -1;

    Node(NodeType type, List<Node> children, Object content)
    {
        this.type = type;
        this.children = children;
        this.content  = content;
    }

    public NodeType getNodeType() { return type; }

    public int getSubtreeCount() { return children.size(); }

    public Node getSubtree(int idx) { return children.get(idx); }

    public void setStateNumber(int stateNumber) { this.stateNumber = stateNumber; }

    public int getStateNumber() { return this.stateNumber; }

    public void setTransitionTableLeaf(Object state) { this.transition = state; }

    public Object getTransitionTableLeaf() { return this.transition; }

    public Object getContent() { return this.content; }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder();
        result.append(type);

        if (content != null) {
            result.append(String.format("(%s)", content));
        }

        if (transition != null) {
            result.append(String.format("{%s}",transition));
        }

        if (children != null && children.size() > 0) {
            result.append(children);
        }

        return result.toString();
    }
}
