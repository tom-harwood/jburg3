package jburg;

public interface BurgInput<NodeType>
{
    public NodeType             getNodeType();
    public int                  getSubtreeCount();
    public BurgInput<NodeType>  getSubtree(int idx);
    public int                  getStateNumber();
    public void                 setStateNumber(int stateNumber);
}
