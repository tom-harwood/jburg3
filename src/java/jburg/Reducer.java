package jburg;

import java.lang.reflect.Array;
import java.util.*;

/**
 * A Reducer is the actual tree parsing automaton.
 * The reducer works in two passes:
 * <li> label(node) traverses the input tree and
 * assigns each node a state number.
 * <li> reduce(node) traverses the tree a second
 * time, running the productions specified by each
 * node's state number to rewrite the tree as
 * specified by the productions.
 */
public class Reducer<Nonterminal, NodeType>
{
    /**
     * The tree parsing automaton drives the visitor by invoking
     * methods as it walks the tree; the visitor also hosts the
     * semantic predicate methods, which are called by the labeling pass.
     */
    final Object visitor;

    /**
     * The production table this Reducer will use to derive its inputs.
     */
    final ProductionTable<Nonterminal, NodeType> productionTable;

    /**
     * Construct a Reducer.
     * @param visitor           the visitor.
     * @param productionTable   the productions this reducer can use to derive its inputs.
     */
    public Reducer(Object visitor, ProductionTable<Nonterminal, NodeType> productionTable)
    {
        this.visitor = visitor;
        this.productionTable = productionTable;
    }

    /**
     * First pass: label a tree.
     * @param node the root of the tree to label.
     */
    public void label(BurgInput<Nonterminal, NodeType> node)
    throws Exception
    {
        productionTable.label(node, visitor, ProductionTable.LABEL_DEEP);
    }

    /**
     * Can the given node produce the given nonterminal?
     * @param node  the node of interest.
     * @param goal  the nonterminal of interest.
     * @return true if node's state table entry can produce goalNt.
     */
    public boolean canProduce(BurgInput<Nonterminal, NodeType> node, Nonterminal goal)
    {
        State<Nonterminal,NodeType> state = node != null? productionTable.getState(node.getStateNumber()): productionTable.getNullPointerState();
        return state.getCost(goal) < Integer.MAX_VALUE;
    }

    /**
     * Second pass: reduce the tree to the desired goal.
     * @param node the root of the tree.
     * @param goal the nonterminal corresponding to the
     * desired result object.
     * @return the result of deriving the tree using the
     * productions specified by each node's state.
     */
    public Object reduce(BurgInput<Nonterminal, NodeType> node, Nonterminal goal)
    throws Exception
    {
        // TODO: Run the entire algorithm iteratively, using the stack.
        Stack<Production<Nonterminal>> productions = new Stack<Production<Nonterminal>>();
        return reduce(node, goal, productions);
    }

    /**
     * Recursively reduce subtrees.
     * @param node the root of a subtree.
     * @param goal the subtree's state.
     * @param pendingProductions    a stack of productions whose
     * antecedent production has yet to be completed; passed here
     * to avoid extra object creation, and eventually to be used
     * to run the algorithm iteratively.
     * @return the result of deriving the subtree.
     * @throws Exception from the production's semantic action routines,
     * and a few diagnostics for unlabeled trees or mismatched parameters.
     */
    @SuppressWarnings("unchecked")
    private Object reduce(BurgInput<Nonterminal, NodeType> node, Nonterminal goal, Stack<Production<Nonterminal>> pendingProductions)
    throws Exception
    {
        State<Nonterminal,NodeType> state;

        if (node != null) {
            state = (State<Nonterminal,NodeType>)node.getTransitionTableLeaf();

            if (state == null) {
                // Debugging: uncomment to see why the first pass didn't label the node.
            	label(node);
                throw new IllegalStateException(String.format("Unlabeled node %s",node));
            }
        } else {
            state = productionTable.getNullPointerState();
        }

        // Run pre-callbacks on any closures to get to
        // the pattern matcher or abnormal node handler.
        Production<Nonterminal> current = state.getProduction(goal);

        while(current instanceof Closure) {
            if (current.preCallback != null) {
                current.preCallback.invoke(visitor, node, goal);
            }
            pendingProductions.push(current);
            current = state.getProduction(((Closure<Nonterminal>)current).source);
        }

        if (current.preCallback != null) {
            current.preCallback.invoke(visitor, node, goal);
        }

        Object result = null;

        // Reduce children and collect results
        if (state instanceof ErrorState) {
            result = handleError(node, goal, pendingProductions);
        } else if (current instanceof NullPointerProduction) {
            if (current.postCallback != null) {
                result = current.postCallback.invoke(visitor, node, goal);
            }
        } else if (current.postCallback != null) {

            assert current instanceof PatternMatcher: String.format("Expected PatternMatcher, got %s\n", current);
            @SuppressWarnings("unchecked")
            PatternMatcher<Nonterminal, NodeType> patternMatcher = (PatternMatcher<Nonterminal, NodeType>)current;

            int formalCount = current.postCallback.getParameterCount();
            // The actual parameters are the root of the subtree itself,
            // plus the result of reducing each of the root's children.
            int actualCount = node.getSubtreeCount() + 1;

            if (!current.postCallback.isVarArgs()) {

                switch(node.getSubtreeCount()) {
                    case 0:
                        result = current.postCallback.invoke(visitor, node);
                        break;

                    case 1:
                        result = current.postCallback.invoke(visitor, node, reduce(node.getSubtree(0), patternMatcher.getNonterminal(0)));
                        break;

                    case 2:
                        result = current.postCallback.invoke(
                            visitor,
                            node,
                            reduce(node.getSubtree(0), patternMatcher.getNonterminal(0)),
                            reduce(node.getSubtree(1), patternMatcher.getNonterminal(1))
                            );
                        break;

                    default: {

                        Object[] actuals = new Object[formalCount];
                        actuals[0] = node;

                        if (formalCount == actualCount) {

                            for (int i = 0; i < node.getSubtreeCount(); i++) {
                                actuals[i+1] = reduce(node.getSubtree(i), patternMatcher.getNonterminal(i));
                            }

                        } else {
                            throw new IllegalStateException(String.format("Method %s expected %d actuals, received %d", current.postCallback, formalCount, actualCount));
                        }

                        result = current.postCallback.invoke(visitor, actuals);
                    }
                }
            } else if (actualCount >= formalCount - 1) {

                Object[] actuals = new Object[formalCount];
                int variadicFormalPos = formalCount - 1;
                int lastFixedSubtree = variadicFormalPos - 1;
                actuals[0] = node;

                for (int i = 0; i < lastFixedSubtree; i++) {
                    actuals[i+1] = reduce(node.getSubtree(i), patternMatcher.getNonterminal(i));
                }

                int nVarArgs = Math.max(0, actualCount - variadicFormalPos);
                Class<?> variadicFormalClass = (Class<?>)current.postCallback.getVariadicComponentType();
                Object variadicActuals = actuals[variadicFormalPos] = Array.newInstance(variadicFormalClass, nVarArgs);

                for (int i = 0; i < nVarArgs; i++) {
                    Object actual = reduce(node.getSubtree(i+lastFixedSubtree), patternMatcher.getNonterminal(i+lastFixedSubtree));
                    Array.set(variadicActuals, i, actual);
                }

                result = current.postCallback.invoke(visitor, actuals);

            } else {
                throw new IllegalStateException(String.format("Method %s expected %d actuals, received %d", current.postCallback, formalCount, actualCount));
            }
        } else {
            result = null;

            if (node != null) {
                // Reduce the children; there may be side effects.
                assert current instanceof PatternMatcher: String.format("Expected PatternMatcher, got %s\n", current);
                @SuppressWarnings("unchecked")
                PatternMatcher<Nonterminal, NodeType> patternMatcher = (PatternMatcher<Nonterminal, NodeType>)current;

                for (int i = 0; i < node.getSubtreeCount(); i++) {
                    reduce(node.getSubtree(i), patternMatcher.getNonterminal(i));
                }
            }
        }

        while (!pendingProductions.isEmpty()) {
            Production<Nonterminal> closure = pendingProductions.pop();
            if (closure.postCallback != null) {
                result = closure.postCallback.invoke(visitor, node, result);
            }
        }

        return result;
    }

    Object handleError(BurgInput<Nonterminal, NodeType> node, Nonterminal goal, Stack<Production<Nonterminal>> pendingProductions)
    throws Exception
    {
        Production<Nonterminal> current = productionTable.getErrorState().getProduction(goal);

        while(current instanceof Closure) {
            if (current.preCallback != null) {
                current.preCallback.invoke(visitor, node, goal);
            }
            pendingProductions.push(current);
            current = productionTable.getErrorState().getProduction(((Closure<Nonterminal>)current).source);
        }

        Object result = current.preCallback != null?
            current.preCallback.invoke(visitor,node,goal):
            null;

        while (!pendingProductions.isEmpty()) {
            Production<Nonterminal> closure = pendingProductions.pop();
            if (closure.postCallback != null) {
                result = closure.postCallback.invoke(visitor, node, result);
            }
        }

        return result;
    }
}
