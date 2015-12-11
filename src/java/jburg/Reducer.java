package jburg;

import java.util.*;

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
    throws Exception
    {
        // TODO: Run the entire algorithm iteratively, using the stack.
        Stack<Production<Nonterminal>> productions = new Stack<Production<Nonterminal>>();
        return reduce(node, goal, productions);
    }

    private Object reduce(BurgInput<NodeType> node, Nonterminal goal, Stack<Production<Nonterminal>> pendingProductions)
    throws Exception
    {
        State<Nonterminal,NodeType> state = productionTable.getState(node.getStateNumber());

        // Run pre-callbacks on any closures to get to the pattern matcher.
        Production<Nonterminal> current = state.getProduction(goal);

        while(current instanceof Closure) {
            // TODO: pre-callbacks.
            pendingProductions.push(current);
            current = state.getProduction(((Closure<Nonterminal>)current).source);
        }

        // TODO: Run the pattern matcher's pre-callback

        Object result = null;

        // Reduce children and collect results
        if (current.postCallback != null) {

            @SuppressWarnings("unchecked")
			PatternMatcher<Nonterminal, NodeType> patternMatcher = (PatternMatcher<Nonterminal, NodeType>)current;

            switch(node.getSubtreeCount()) {
                case 0:
                    result = current.postCallback.invoke(node);
                    break;
                case 1:
                    result = current.postCallback.invoke(node, reduce(node.getSubtree(0), patternMatcher.getNonterminal(0)));
                    break;
                case 2:
                    result = current.postCallback.invoke(node, reduce(node.getSubtree(0), patternMatcher.getNonterminal(0)), reduce(node.getSubtree(1), patternMatcher.getNonterminal(1)));
                    break;
                default:
                    throw new IllegalStateException(String.format("Unimplemented: child arity %d", node.getSubtreeCount()));
            }
        }

        while (!pendingProductions.isEmpty()) {
            Production<Nonterminal> closure = pendingProductions.pop();
            if (closure.postCallback != null) {
                result = closure.postCallback.invoke(node, result);
            }
        }

        return result;
    }
}
