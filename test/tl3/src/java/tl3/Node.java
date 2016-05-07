package tl3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jburg.BurgInput;
import jburg.State;

class Node implements BurgInput<Nonterminal, NodeType>
{
    final NodeType      nodeType;
    final List<Node>    children;
    final Object        content;

    int stateNumber = -1;
    State<Nonterminal,NodeType> transition;

    Node(NodeType nodeType)
    {
        this.nodeType = nodeType;
        this.children = new ArrayList<Node>();
        this.content  = null;
    }

    Node(NodeType nodeType, int childCount)
    {
        this(nodeType);

        for (int i = 0; i < childCount; i++) {
            this.children.add(null);
        }
    }

    Node(NodeType nodeType, Object content)
    {
        this.nodeType = nodeType;
        this.children = Collections.emptyList();
        this.content  = content;
    }

    public NodeType getNodeType()
    {
        return nodeType;
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

    public void setTransitionTableLeaf(State<Nonterminal,NodeType> state)
    {
        this.transition = state;
    }

    public State<Nonterminal,NodeType> getTransitionTableLeaf()
    {
        return this.transition;
    }

    public Object getContent()
    {
        return this.content;
    }

    @Override
    public String toString()
    {
        if (content != null) {
            return String.format("%s(%d)%s", nodeType, stateNumber, content);
        } else {
            return String.format("%s(%d)%s", nodeType, stateNumber, children);
        }
    }

    void setChild(int idx, Node child)
    {
        children.set(idx, child);
    }
}
