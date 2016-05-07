package jburg;
import java.util.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

abstract class TransitionTableVisitor<Node, Nonterminal, NodeType>
{
    protected final Nonterminal[]   emptyNonterminals;
    protected final Class<?>        nonterminalClass;
    protected final Class<?>        nodeTypeClass;

    // FIXME: Remove this once Node is better established.
    class Node
    {
        String get(String attrName)
        {
            return "Potzrebie";
        }
    }

    @SuppressWarnings("unchecked")
    TransitionTableVisitor(Class<?> nonterminalClass, Class<?> nodeTypeClass)
    {
        this.nonterminalClass = nonterminalClass;
        this.nodeTypeClass = nodeTypeClass;
        this.emptyNonterminals = (Nonterminal[])Array.newInstance(nonterminalClass, 0);
    }

    /**
     * Pass a reduced subtree to this node's parent.
     */
    public Object passthrough(Node node, Object arg)
    {
        return arg;
    }

    public Object packageVarArgs(Node node, Object... args)
    {
        if (args.length > 0 && args[0] != null) {
            Object result = Array.newInstance(args[0].getClass(), args.length);

            for (int i = 0; i < args.length; i++) {
                Array.set(result, i, args[i]);
            }

            return result;
        } else {
            return null;
        }
    }

    abstract Object parseVariadicLeafPlane(Node node, Object variadicMarker, HyperPlane<Nonterminal, NodeType> finalDimension);


    public Method parseMethod(Node node, Class<?>[] parameterTypes)
    throws Exception
    {
        Class<?> receiverClass = Class.forName(node.get("class"));
        Method result = receiverClass.getDeclaredMethod(node.get("name"),parameterTypes);
        return result;
    }

    protected Nonterminal parseNonterminal(Node node)
    {
        return getNonterminal(node.get("nonterminal"));
    }

    protected Class<?> parseClass(Node node)
    throws Exception
    {
        return Class.forName(node.get("class"));
    }

    Closure<Nonterminal> parseClosureWithPre(Node node, Method preCallback)
    {
        return parseClosureWithPreAndPost(node, preCallback, null);
    }

    Closure<Nonterminal> parseClosureWithPost(Node node, Method postCallback)
    {
        return parseClosureWithPreAndPost(node, null, postCallback);
    }

    abstract Closure<Nonterminal> parseClosureWithPreAndPost(Node node, Method preCallback, Method postCallback);

    @SuppressWarnings("unchecked")
    abstract PatternMatcher<Nonterminal,NodeType> parsePatternMatcher(Node node, Object childTypes, Method preCallback, Method postCallback);

    abstract PatternMatcher<Nonterminal,NodeType> parsePatternMatcherPost(Node node, Object childTypes, Method postCallback);

    PatternMatcher<Nonterminal,NodeType> parsePatternMatcherPre(Node node, Object childTypes, Method preCallback)
    {
        return parsePatternMatcher(node, childTypes, null, preCallback);
    }

    PatternMatcher<Nonterminal,NodeType> parsePatternMatcherLeafPreAndPost(Node node, Method preCallback, Method postCallback)
    {
        return parsePatternMatcher(node, emptyNonterminals, preCallback, postCallback);
    }

    PatternMatcher<Nonterminal,NodeType> parsePatternMatcherLeafPost(Node node, Method postCallback)
    {
        return parsePatternMatcher(node, emptyNonterminals, null, postCallback);
    }

    PatternMatcher<Nonterminal,NodeType> parsePatternMatcherLeafPre(Node node, Method preCallback)
    {
        return parsePatternMatcher(node, emptyNonterminals, preCallback, null);
    }

    @SuppressWarnings("unchecked")
    abstract Object buildStateTable(Node node, State<Nonterminal, NodeType>... states);
    
    abstract Object buildProductionTable(Node node, State<Nonterminal, NodeType>[] stateTable, Operator<Nonterminal, NodeType>[] operatorTable);


    abstract Object parseEntry(Node node, PredicatedState<Nonterminal, NodeType> state);

    @SuppressWarnings("unchecked")
    abstract HyperPlane<Nonterminal, NodeType> parseUnaryFinalDimension(Node node, Object... states);

    abstract Object parseFinalDimension(Node node, Integer[] mappedStates, HyperPlane<Nonterminal, NodeType> hyperPlane);

    abstract Object parseVariadicFinalDimension(Node node, Integer[] mappedStates, HyperPlane<Nonterminal, NodeType> finalDimension);

    @SuppressWarnings("unchecked")
    abstract HyperPlane<Nonterminal, NodeType> createRootHyperplane(Node node, Object[] nextDimension);

    abstract Object createHyperplaneSingleMapping(Node node, Integer[] mappedStates, Object[] nextDimension);

    abstract State<Nonterminal, NodeType> parseState(Node node, PatternMatcher<Nonterminal, NodeType>[] patternMatchers, Object[] costs, Closure<Nonterminal>[] closures, Method[] predicates);

    @SuppressWarnings("unchecked")
    State<Nonterminal, NodeType> parseState(Node node, PatternMatcher<Nonterminal, NodeType>[] patternMatchers, Object[] costs, Method[] predicates)
    {
        return parseState(node, patternMatchers, costs, new Closure[0], predicates);
    }

    @SuppressWarnings("unchecked")
    State<Nonterminal, NodeType> parseState(Node node, Method[] predicates)
    {
        return parseState(node, new PatternMatcher[0], null, new Closure[0], predicates);
    }

    abstract Object parseCost(Node node);

    abstract Object parseEmptyPredicates(Node node);

    abstract Operator<Nonterminal, NodeType> parseLeafOperator(Node node, PredicatedState<Nonterminal, NodeType> predicatedState);

    abstract Operator<Nonterminal, NodeType> parseNonLeafOperator(Node node, HyperPlane<Nonterminal, NodeType> rootPlane);

    abstract Integer parseStateNumber(Node node);

    abstract Object parsePredicatedState(Node node, Integer... states);

    @SuppressWarnings("unchecked")
    Nonterminal getNonterminal(String ntName)
    {
        for (Object nt: nonterminalClass.getEnumConstants()) {
            if (nt.toString().equals(ntName)) {
                return (Nonterminal)nt;
            }
        }

        throw new IllegalArgumentException(String.format("enumeration %s does not contain %s", nonterminalClass, ntName));
    }

    @SuppressWarnings("unchecked")
    NodeType getNodeType(String ntName)
    {
        for (Object nt: nodeTypeClass.getEnumConstants()) {
            if (nt.toString().equals(ntName)) {
                return (NodeType)nt;
            }
        }

        throw new IllegalArgumentException(String.format("enumeration %s does not contain %s", nodeTypeClass, ntName));
    }

    abstract Method getPostCallback(String methodName, Class<?>... formalTypes);
}
