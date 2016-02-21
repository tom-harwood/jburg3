package jburg;

import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.util.*;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class TransitionTableLoader<Nonterminal, NodeType> extends DefaultHandler
{
    Stack<HyperPlane<Nonterminal, NodeType>> hyperplanes = new Stack<HyperPlane<Nonterminal, NodeType>>();

    public ProductionTable<Nonterminal, NodeType> load(String uri, Class<?> nonterminalClass, Class<?> nodeTypeClass)
    throws Exception
    {
        // Load the table as a Node tree.
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.parse(uri);

        // Parse the Node tree into a production table.
        DumpParser dumpParser = new DumpParser(this.rootNode, nonterminalClass, nodeTypeClass);
        return dumpParser.getProductionTable();
    }

    enum DumpType
    {
        burmDump,
        childType,
        childTypes,
        closure,
        closures,
        constituentState,
        cost,
        costMap,
        entry,
        finalDimension,
        hyperPlane,
        mappedState,
        mappedStates,
        method,
        nextDimension,
        operator,
        parameter,
        parameterTypes,
        pattern,
        patterns,
        postCallback,
        preCallback,
        predicatedState,
        predicates,
        state,
        stateTable,
        transitionTable,
        variadic,
    }

    enum Load
    {
        Cost,
        CostMap,
        ChildType,
        ChildTypes,
        Closure,
        Closures,
        ConstituentState,
        Entry,
        FinalDimension,
        RootHyperPlane,
        MappedState,
        MappedStates,
        Method,
        NextDimension,
        Operator,
        ParameterTypes,
        Parameter,
        Pattern,
        Patterns,
        PostCallback,
        PreCallback,
        PredicatedState,
        Predicates,
        ProductionTable,
        State,
        StateTable,
        TransitionTable,
        Variadic,
    }

    private class Node implements BurgInput<DumpType>
    {
        final DumpType              nodeType;
        final Map<String,String>    attributes = new HashMap<String,String>();
        final List<Node>            children = new ArrayList<Node>();

        int                         stateNumber;

        Node(String localName, Attributes atts)
        {
            this.nodeType = DumpType.valueOf(localName);

            for (int i = 0; i < atts.getLength(); i++) {
                attributes.put(atts.getLocalName(i),atts.getValue(i));
            }
        }

        String get(String key)
        {
            return this.attributes.get(key);
        }

        @Override
        public int getStateNumber()                 { return this.stateNumber; }
        @Override
        public void setStateNumber(int stateNumber) { this.stateNumber = stateNumber; }
        @Override
        public int getSubtreeCount()                { return this.children.size(); }
        @Override
        public Node getSubtree(int idx)             { return this.children.get(idx); }
        @Override
        public DumpType getNodeType()               { return this.nodeType; }

        @SuppressWarnings("unused")
		void dump(java.io.PrintStream out)
        {
            dump(out,0);
        }

        void dump(java.io.PrintStream out, int indent)
        {
            for (int i = 0; i < indent * 2;i++) out.print(" ");
            out.printf("%s %d\n",  this.nodeType, this.stateNumber);
            for (Node child:children) {
                child.dump(out, indent+1);
            }
        }

        @Override
        public String toString()
        {
            return String.format("%s%s", this.nodeType, this.attributes);
        }
    }

    Node rootNode = null;
    Stack<Node> nodeStack = new Stack<Node>();

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException
    {
        Node n = new Node(localName,atts);
        if (!nodeStack.isEmpty()) {
            nodeStack.peek().children.add(n);
        } else {
            rootNode = n;
        }

        nodeStack.push(n);
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
    {
        nodeStack.pop();
    }

    class CostEntry
    {
        final Nonterminal   nt;
        final int           cost;

        CostEntry(Nonterminal nt, int cost)
        {
            this.nt = nt;
            this.cost = cost;
        }
    }

    class DumpParser
    {
        Class<?> getArrayArgs(Class<?> elementType)
        {
            return Array.newInstance(elementType, 0).getClass();
        }

        ProductionTable<Load,DumpType> loadProductions(TTBuilder builder)
        throws Exception
        {
            Method passthrough      = builder.getPostCallback("passthrough", Object.class);
            Method packageVarArgs   = builder.getPostCallback("packageVarArgs", getArrayArgs(Object.class));
            Method parseClass       = builder.getPostCallback("parseClass");
            Method noop             = null;

            ProductionTable<Load,DumpType> productions = new ProductionTable<Load,DumpType>();

            // Production table
            productions.addVarArgsPatternMatch(
                Load.ProductionTable, DumpType.burmDump,
                builder.getPostCallback("buildProductionTable", getArrayArgs(State.class), getArrayArgs(Operator.class)),
                Load.StateTable,
                Load.TransitionTable
            );

            // State table
            productions.addVarArgsPatternMatch(
                Load.StateTable, DumpType.stateTable,
                builder.getPostCallback("buildStateTable", getArrayArgs(State.class)),
                Load.State
            );
            productions.addPatternMatch(
                Load.State, DumpType.state,
                builder.getPostCallback("parseState", getArrayArgs(Method.class)),
                Load.Predicates
            );
            productions.addPatternMatch(
                Load.State, DumpType.state,
                builder.getPostCallback("parseState", getArrayArgs(PatternMatcher.class), getArrayArgs(CostEntry.class), getArrayArgs(Method.class)),
                Load.Patterns, Load.CostMap, Load.Predicates
            );
            productions.addPatternMatch(
                Load.State, DumpType.state,
                builder.getPostCallback("parseState", getArrayArgs(PatternMatcher.class), getArrayArgs(CostEntry.class), getArrayArgs(Closure.class), getArrayArgs(Method.class)),
                Load.Patterns, Load.CostMap, Load.Closures, Load.Predicates
            );

            productions.addVarArgsPatternMatch(Load.Predicates, DumpType.predicates, packageVarArgs, Load.Method);
            productions.addPatternMatch(Load.Predicates, DumpType.predicates, builder.getPostCallback("parseEmptyPredicates"));

            // Patterns
            productions.addVarArgsPatternMatch(Load.Patterns, DumpType.patterns, packageVarArgs, Load.Pattern);

            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcherLeafPost", Method.class),
                Load.PostCallback
            );
            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcherLeafPre", Method.class),
                Load.PreCallback
            );
            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcherLeafPreAndPost", Method.class, Method.class),
                Load.PreCallback, Load.PostCallback
            );
            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcherPost", Object.class, Method.class),
                Load.ChildTypes, Load.PostCallback
            );
            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcherPre", Object.class, Method.class),
                Load.ChildTypes, Load.PreCallback
            );
            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcher", Object.class, Method.class, Method.class),
                Load.ChildTypes, Load.PreCallback, Load.PostCallback
            );

            productions.addVarArgsPatternMatch(Load.ChildTypes, DumpType.childTypes, packageVarArgs, Load.ChildType);
            productions.addPatternMatch(Load.ChildType, DumpType.childType, builder.getPostCallback("parseNonterminal"));

            // Cost map
            productions.addVarArgsPatternMatch(Load.CostMap, DumpType.costMap, packageVarArgs, Load.Cost);
            productions.addPatternMatch(Load.Cost, DumpType.cost, builder.getPostCallback("parseCost"));

            // Closures
            productions.addVarArgsPatternMatch(Load.Closures, DumpType.closures, packageVarArgs, Load.Closure);
            productions.addPatternMatch(Load.Closure, DumpType.closure, builder.getPostCallback("parseClosureWithPost", Method.class), Load.PostCallback);
            productions.addPatternMatch(Load.Closure, DumpType.closure, builder.getPostCallback("parseClosureWithPre", Method.class), Load.PreCallback);
            productions.addPatternMatch(Load.Closure, DumpType.closure, builder.getPostCallback("parseClosureWithPreAndPost", Method.class, Method.class), Load.PreCallback, Load.PostCallback);

            // Callbacks
            productions.addPatternMatch(Load.PreCallback, DumpType.preCallback, passthrough, Load.Method);
            productions.addPatternMatch(Load.PostCallback, DumpType.postCallback, passthrough, Load.Method);

            // Method
            productions.addPatternMatch(Load.Method, DumpType.method, builder.getPostCallback("parseMethod", getArrayArgs(Class.class)), Load.ParameterTypes);
            productions.addVarArgsPatternMatch(Load.ParameterTypes, DumpType.parameterTypes, packageVarArgs, Load.Parameter);
            productions.addPatternMatch(Load.Parameter, DumpType.parameter, parseClass);

            // Transition table
            productions.addVarArgsPatternMatch(
                Load.TransitionTable, DumpType.transitionTable,
                packageVarArgs,
                Load.Operator
            );

            productions.addPatternMatch(
                Load.Operator, DumpType.operator,
                builder.getPostCallback("parseNonLeafOperator", HyperPlane.class),
                Load.RootHyperPlane
            );

            productions.addPatternMatch(
                Load.Operator,
                DumpType.operator,
                builder.getPostCallback("parseLeafOperator", PredicatedState.class),
                Load.PredicatedState
            );

            /*
             * Hyper plane roots
             */
            // Leaf hyperplane, non variadic
            productions.addPatternMatch(
                Load.RootHyperPlane, DumpType.hyperPlane,
                passthrough,
                Load.FinalDimension
            );

            // Leaf hyperplane, variadic
            productions.addPatternMatch(
                Load.RootHyperPlane, DumpType.hyperPlane,
                builder.getPostCallback("parseVariadicLeafPlane", Object.class, HyperPlane.class),
                Load.Variadic, Load.FinalDimension
            );

            // Non-leaf root hyperplane
            productions.addPatternMatch(
                Load.RootHyperPlane, DumpType.hyperPlane,
                builder.getPostCallback("createRootHyperplane", getArrayArgs(HyperPlaneDesc.class)),
                Load.NextDimension
            );

            // Final dimension of a unary operator
            productions.addVarArgsPatternMatch(
                Load.FinalDimension, DumpType.finalDimension,
                builder.getPostCallback("parseUnaryFinalDimension", getArrayArgs(Object.class)),
                Load.Entry
            );

            // Final dimension of an operator of arity > 1
            productions.addPatternMatch(
                Load.NextDimension, DumpType.hyperPlane,
                builder.getPostCallback("parseFinalDimension", getArrayArgs(Integer.class), HyperPlane.class),
                Load.MappedStates, Load.FinalDimension
            );

            // Variadic final dimension of operator arity > 1
            productions.addPatternMatch(
                Load.NextDimension, DumpType.hyperPlane,
                builder.getPostCallback("parseVariadicFinalDimension", getArrayArgs(Integer.class), HyperPlane.class),
                Load.Variadic, Load.MappedStates, Load.FinalDimension
            );

            // Next dimension, only one mapping
            productions.addPatternMatch(
                Load.NextDimension, DumpType.hyperPlane,
                builder.getPostCallback("createHyperplaneSingleMapping", getArrayArgs(Integer.class), getArrayArgs(HyperPlaneDesc.class)),
                Load.MappedStates, Load.NextDimension
            );

            // Next dimension, more than one mapping
            productions.addVarArgsPatternMatch(
                Load.NextDimension, DumpType.nextDimension,
                packageVarArgs,
                Load.NextDimension
            );

            // Mapped states are arrays of integers.
            productions.addVarArgsPatternMatch(Load.MappedStates, DumpType.mappedStates, packageVarArgs, Load.MappedState);
            productions.addPatternMatch(Load.MappedState, DumpType.mappedState, builder.getPostCallback("parseStateNumber"));

            productions.addPatternMatch(
                Load.Entry, DumpType.entry,
                builder.getPostCallback("parseEntry", PredicatedState.class),
                Load.PredicatedState
            );
            // Variadic marker only needs to be recognized.
            productions.addPatternMatch(Load.Variadic, DumpType.variadic, noop);

            // Predicated state
            productions.addVarArgsPatternMatch(
                Load.PredicatedState, DumpType.predicatedState, 
                builder.getPostCallback("parsePredicatedState", getArrayArgs(Integer.class)),
                Load.ConstituentState
            );

            productions.addPatternMatch(
                Load.ConstituentState, DumpType.constituentState,
                builder.getPostCallback("parseStateNumber")
            );

            productions.generateStates();
            return productions;
        }

        ProductionTable<Nonterminal,NodeType> result;
        /**
         * The caller has to provide this because Java uses type erasure for generics,
         * so we have no direct access to the Nonterminal class.
         */
        Class<?> nonterminalClass;

        /**
         * The caller has to provide this because Java uses type erasure for generics,
         * so we have no direct access to the NodeType class.
         */
        Class<?> nodeTypeClass;

        DumpParser(Node root, Class<?> nonterminalClass, Class<?> nodeTypeClass)
        throws Exception
        {
            this.nonterminalClass = nonterminalClass;
            this.nodeTypeClass = nodeTypeClass;

            TTBuilder builder = new TTBuilder(nonterminalClass, nodeTypeClass);
            Reducer<Load,DumpType> reducer = new Reducer<Load,DumpType>(builder, loadProductions(builder));
            reducer.label(root);

            if (reducer.canProduce(root, Load.ProductionTable)) {
                reducer.reduce(root, Load.ProductionTable);
                this.result = builder.getProductionTable();
            } else {
                System.err.printf("Load failed:\n");
                root.dump(System.err);
                throw new IllegalStateException("Unable to load production table.");
            }
        }

        ProductionTable<Nonterminal,NodeType> getProductionTable()
        {
            assert this.result != null;
            return this.result;
        }
    }

   class HyperPlaneDesc
    {
        HyperPlane<Nonterminal, NodeType>   hyperPlane;
        Integer[]                           mappedStates;
        boolean                             isVarArgs;

        HyperPlaneDesc(HyperPlane<Nonterminal, NodeType> hyperPlane, Integer[] mappedStates, boolean isVarArgs)
        {
            this.hyperPlane     = hyperPlane;
            this.mappedStates   = mappedStates;
            this.isVarArgs      = isVarArgs;
        }
    }

    class EntryDesc
    {
        PredicatedState<Nonterminal, NodeType>  state;
        Integer                                 stateNumber;

        EntryDesc(PredicatedState<Nonterminal, NodeType> state, Integer stateNumber)
        {
            this.state = state;
            this.stateNumber = stateNumber;
        }
    }

    // TODO: Encapsulate in an interface.
    class TTBuilder
    {
        final Nonterminal[] emptyNonterminals;
        List<State<Nonterminal, NodeType>> stateTable = null;

        @SuppressWarnings("unchecked")
        TTBuilder(Class<?> nonterminalClass, Class<?> nodeTypeClass)
        {
            this.nonterminalClass = nonterminalClass;
            this.nodeTypeClass = nodeTypeClass;
            this.emptyNonterminals = (Nonterminal[])Array.newInstance(nonterminalClass, 0);
        }

        ProductionTable<Nonterminal,NodeType> productionTable = new ProductionTable<Nonterminal, NodeType>();
        Class<?> nonterminalClass;
        Class<?> nodeTypeClass;

        ProductionTable<Nonterminal,NodeType> getProductionTable()
        {
            return productionTable;
        }

        Class<?> parseClass(Node node)
        throws Exception
        {
            return Class.forName(node.get("type"));
        }

        Object passthrough(Node node, Object arg)
        {
            return arg;
        }

        Object parseVariadicLeafPlane(Node node, Object variadicMarker, HyperPlane<Nonterminal, NodeType> finalDimension)
        {
            // Create state transitions back to this hyperplane for all mapped states.
            int hyperPlaneIndex = finalDimension.nextDimension.size();
            finalDimension.nextDimension.add(finalDimension);

            for (Integer stateNumber: finalDimension.finalDimIndexMap.keySet()) {
                finalDimension.nextDimIndexMap.put(stateNumber, hyperPlaneIndex);
            }
            return finalDimension;
        }

        Object packageVarArgs(Node node, Object... args)
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

        Method parseMethod(Node node, Class<?>[] parameterTypes)
        throws Exception
        {
            Class<?> receiverClass = Class.forName(node.get("class"));
            Method result = receiverClass.getDeclaredMethod(node.get("name"),parameterTypes);
            return result;
        }

        Closure<Nonterminal> parseClosureWithPre(Node node, Method preCallback)
        {
            return parseClosureWithPreAndPost(node, preCallback, null);
        }

        Closure<Nonterminal> parseClosureWithPost(Node node, Method postCallback)
        {
            return parseClosureWithPreAndPost(node, null, postCallback);
        }

        Closure<Nonterminal> parseClosureWithPreAndPost(Node node, Method preCallback, Method postCallback)
        {
            return new Closure<Nonterminal>(
                getNonterminal(node.get("nonterminal")),
                getNonterminal(node.get("source")),
                Integer.parseInt(node.get("cost")),
                preCallback,
                postCallback
            );
        }

        Nonterminal parseNonterminal(Node node)
        {
            return getNonterminal(node.get("nonterminal"));
        }

        @SuppressWarnings("unchecked")
        PatternMatcher<Nonterminal,NodeType> parsePatternMatcher(Node node, Object childTypes, Method preCallback, Method postCallback)
        {
            return new PatternMatcher<Nonterminal,NodeType>(
                getNonterminal(node.get("nonterminal")),
                null,
                Integer.parseInt(node.get("cost")),
                null,
                preCallback,
                postCallback,
                Boolean.parseBoolean(node.get("variadic")), 
                (Nonterminal[])childTypes
            );
        }

        PatternMatcher<Nonterminal,NodeType> parsePatternMatcherPost(Node node, Object childTypes, Method postCallback)
        {
            return parsePatternMatcher(node, childTypes, null, postCallback);
        }

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
        Object buildStateTable(Node node, State<Nonterminal, NodeType>... states)
        {
            this.stateTable = new ArrayList<State<Nonterminal, NodeType>>();
            for (State<Nonterminal, NodeType> s: states) {
                assert this.stateTable.size() == s.number: String.format("Expected state %d got state %d", this.stateTable.size(), s.number);
                this.stateTable.add(s);
            }

            return states;
        }
        
        Object buildProductionTable(Node node, State<Nonterminal, NodeType>[] stateTable, Operator<Nonterminal, NodeType>[] operatorTable)
        {
            for (State<Nonterminal, NodeType> s: stateTable) {
                this.productionTable.statesInEntryOrder.add(s);
            }

            for (Operator<Nonterminal, NodeType> op: operatorTable) {
                this.productionTable.loadOperator(op);
            }

            return this.productionTable;
        }


        State<Nonterminal, NodeType> parseState(Node node, PatternMatcher<Nonterminal, NodeType>[] patternMatchers, CostEntry[] costs, Closure<Nonterminal>[] closures, Method[] predicates)
        {
            State<Nonterminal, NodeType> result = new State<Nonterminal, NodeType>();
            result.number = Integer.parseInt(node.get("number"));
            
            // Create a scratch "cost table" to rebuild the State's cost data.
            Map<Nonterminal,Long> costMap = new HashMap<Nonterminal,Long>();

            if (costs != null) {

                for (CostEntry ce: costs) {
                    costMap.put((Nonterminal)ce.nt, (long)ce.cost);
                }
            }

            for (PatternMatcher<Nonterminal, NodeType> p: patternMatchers) {
                result.setNonClosureProduction(p, costMap.get(p.target));
            }

            for (Closure<Nonterminal> c: closures) {
                result.addClosure(c);
            }

            return result;
        }

        EntryDesc parseEntry(Node node, PredicatedState<Nonterminal, NodeType> state)
        {
            return new EntryDesc(state, Integer.valueOf(node.get("stateNumber")));
        }

        @SuppressWarnings("unchecked")
        HyperPlane<Nonterminal, NodeType> parseUnaryFinalDimension(Node node, Object... states)
        {
            HyperPlane<Nonterminal, NodeType> hyperPlane = new HyperPlane<Nonterminal, NodeType>();

            for (Object o: states) {
                EntryDesc desc = (EntryDesc) o;
                hyperPlane.loadPredicatedState(desc.stateNumber, desc.state);
            }

            return hyperPlane;
        }

        HyperPlaneDesc parseFinalDimension(Node node, Integer[] mappedStates, HyperPlane<Nonterminal, NodeType> hyperPlane)
        {
            return new HyperPlaneDesc(hyperPlane, mappedStates, false);
        }

        HyperPlaneDesc parseVariadicFinalDimension(Node node, Integer[] mappedStates, HyperPlane<Nonterminal, NodeType> finalDimension)
        {
            finalDimension.makeVariadic();
            return new HyperPlaneDesc(finalDimension, mappedStates, true);
        }

        @SuppressWarnings("unchecked")
        HyperPlane<Nonterminal, NodeType> createRootHyperplane(Node node, HyperPlaneDesc[] nextDimension)
        {
            HyperPlane<Nonterminal, NodeType> rootPlane = new HyperPlane<Nonterminal, NodeType>();

            for (HyperPlaneDesc desc: nextDimension) {
                rootPlane.loadHyperPlane(desc.hyperPlane, desc.mappedStates);
            }

            return rootPlane;
        }

        HyperPlaneDesc createHyperplaneSingleMapping(Node node, Integer[] mappedStates, HyperPlaneDesc[] nextDimension)
        {
            HyperPlane<Nonterminal, NodeType> thisDimension = new HyperPlane<Nonterminal, NodeType>();

            for (HyperPlaneDesc desc: nextDimension) {
                thisDimension.loadHyperPlane(desc.hyperPlane, desc.mappedStates);
            }
            return new HyperPlaneDesc(thisDimension, mappedStates, false);
        }

        @SuppressWarnings("unchecked")
        State<Nonterminal, NodeType> parseState(Node node, PatternMatcher<Nonterminal, NodeType>[] patternMatchers, CostEntry[] costs, Method[] predicates)
        {
            return parseState(node, patternMatchers, costs, new Closure[0], predicates);
        }

        @SuppressWarnings("unchecked")
        State<Nonterminal, NodeType> parseState(Node node, Method[] predicates)
        {
            return parseState(node, new PatternMatcher[0], null, new Closure[0], predicates);
        }

        CostEntry parseCost(Node node)
        {
            return new CostEntry(getNonterminal(node.get("nonterminal")), Integer.parseInt(node.get("cost")));
        }

        Object parseEmptyPredicates(Node node)
        {
            return new Method[0];
        }

        Operator<Nonterminal, NodeType> parseLeafOperator(Node node, PredicatedState<Nonterminal, NodeType> predicatedState)
        {
            Operator<Nonterminal, NodeType> operator = new Operator<Nonterminal, NodeType>(getNodeType(node.get("nodeType")), 0, this.productionTable);
            operator.setLeafState(predicatedState);
            return operator;
        }

        Operator<Nonterminal, NodeType> parseNonLeafOperator(Node node, HyperPlane<Nonterminal, NodeType> rootPlane)
        {
            Operator<Nonterminal, NodeType> operator = new Operator<Nonterminal, NodeType>(getNodeType(node.get("nodeType")), Integer.parseInt(node.get("arity")), this.productionTable);
            operator.transitionTable = rootPlane;
            return operator;
        }

        Integer parseStateNumber(Node node)
        {
            return Integer.parseInt(node.get("stateNumber"));
        }

        Object parsePredicatedState(Node node, Integer... states)
        {
            List<State<Nonterminal, NodeType>> stateList = new ArrayList<State<Nonterminal, NodeType>>();

            for (Integer stateNumber: states) {
                stateList.add(stateTable.get(stateNumber));
            }

            return new PredicatedState<Nonterminal, NodeType>(stateList);
        }

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

        Method getPostCallback(String methodName, Class<?>... formalTypes)
        {
            Class<?>[] formalsWithNode = new Class<?>[formalTypes.length+1];
            formalsWithNode[0] = Node.class;
            for (int i = 0; i < formalTypes.length; i++) {
                formalsWithNode[i+1] = formalTypes[i];
            }

            try {
                return TTBuilder.class.getDeclaredMethod(methodName, formalsWithNode);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new IllegalStateException(ex);
            }
        }
    }
}
