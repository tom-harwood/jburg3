package jburg;

public class Reducer<Nonterminal, NodeType>
{

    final ProductionTable<Nonterminal, NodeType> productionTable;

    public Reducer(ProductionTable<Nonterminal, NodeType> productionTable)
    {
        this.productionTable = productionTable;
    }

    public void label(BurgInput<NodeType> node)
    {
        Operator<Nonterminal, NodeType> op = productionTable.getOperator(node.getNodeType(), node.getSubtreeCount());

        if (op != null) {
            int subtreeCount = node.getSubtreeCount();

            if (subtreeCount > 0) {

                for (int i = 0; i < subtreeCount; i++) {
                    label(node.getSubtree(i));
                }

                // Navigate the operator's transition table.
                HyperPlane<Nonterminal, NodeType> current = op.transitionTable;

                for (int dim = 0; dim < node.getSubtreeCount(); dim++) {
                    RepresenterState<Nonterminal, NodeType> rs = op.getRepresenterState(node.getSubtree(dim).getStateNumber(), dim);

                    if (dim < subtreeCount-1) {
                        current = current.getNextDimension(rs);
                    } else {
                        node.setStateNumber(current.getResultState(rs).getStateNumber());
                    }
                }

            } else {
                node.setStateNumber(op.getLeafState().getStateNumber());
            }
        }
    }

    public Object reduce(BurgInput<NodeType> node, Nonterminal goal)
    {
        throw new IllegalStateException("unimplemented");

        // Conceptually, this is simple:
        // 1. Run pre-callbacks on any closures to get to the pattern matcher
        // 1a. Run the pattern matcher's pre-callback
        // 2. Reduce children and collect results
        // 3. Run the pattern matcher's post callback
        // 4. Return whatever the post callback returned

        // Getting the arguments properly marshaled is going to be amusing.
        // It's worth suffering some pain here to make the callback API
        // easier to work with; that will require making inferences about
        // the arguments by inspecting the callbacks' method signatures.
    }
}
