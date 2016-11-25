package jburg;

import jburg.semantics.HostRoutine;
import java.lang.reflect.Array;
import java.util.*;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class TransitionTableLoader<Nonterminal, NodeType> extends DefaultHandler
{
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
        cost,
        costMap,
        errorHandler,
        finalDimension,
        finalDimIndexMap,
        hyperPlane,
        index,
        leafState,
        method,
        nextDimIndexMap,
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
        ErrorHandler,
        FinalDimension,
        FinalDimIndexMap,
        LeafState,
        Index,
        TransitionPlane,
        MappedState,
        MappedStates,
        Method,
        NextDimension,
        NextDimIndexMap,
        Operator,
        ParameterTypes,
        Parameter,
        Pattern,
        Patterns,
        PostCallback,
        PreCallback,
        TransitionTableLeaf,
        Predicates,
        ProductionTable,
        State,
        TransitionTable,
        Variadic,
    }

    private class Node implements BurgInput<Load, DumpType>
    {
        final DumpType              nodeType;
        final Map<String,String>    attributes = new HashMap<String,String>();
        final List<Node>            children = new ArrayList<Node>();

        int                         stateNumber;
        Object                      transitionTableLeaf;

        Node(String localName, Attributes atts)
        {
            this.nodeType = DumpType.valueOf(localName);

            for (int i = 0; i < atts.getLength(); i++) {
                attributes.put(atts.getLocalName(i),atts.getValue(i));
            }
        }

        private String get(String attrName)
        {
            return this.attributes.get(attrName);
        }

        boolean hasAttribute(String attrName)
        {
            return this.attributes.containsKey(attrName);
        }

        boolean getBooleanAttr(String attrName)
        {
            assert hasAttribute(attrName): String.format("Attribute %s not present", attrName);
            return Boolean.valueOf(get(attrName));
        }

        Integer getIntegerAttr(String attrName)
        {
            assert hasAttribute(attrName): String.format("Attribute %s not present", attrName);
            return Integer.valueOf(get(attrName));
        }

        String getStringAttr(String attrName)
        {
            assert hasAttribute(attrName): String.format("Attribute %s not present", attrName);
            return get(attrName);
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

        public Object getTransitionTableLeaf()
        {
            return this.transitionTableLeaf;
        }

        public void setTransitionTableLeaf(Object transitionTableLeaf)
        {
            this.transitionTableLeaf = transitionTableLeaf;
        }

        @SuppressWarnings("unused")
		void dump(java.io.PrintStream out)
        {
            dump(out,0);
        }

        @SuppressWarnings("unchecked")
        void dump(java.io.PrintStream out, int indent)
        {
            for (int i = 0; i < indent * 2;i++) out.print(" ");
            out.print(this.nodeType);
            if (this.transitionTableLeaf != null) {
                out.printf(" %d %s\n", this.stateNumber, ((State<Nonterminal,NodeType>)this.transitionTableLeaf).getNonterminals());
            } else {
                out.println(" --");
            }
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
            HostRoutine passthrough      = builder.getPostCallback("passthrough", Object.class);
            HostRoutine packageVarArgs   = builder.getPostCallback("packageVarArgs", getArrayArgs(Object.class));
            HostRoutine parseClass       = builder.getPostCallback("parseClass");
            HostRoutine noop             = null;

            ProductionTable<Load,DumpType> productions = new ProductionTable<Load,DumpType>();

            // Production table
            productions.addVarArgsPatternMatch(
                Load.ProductionTable, DumpType.burmDump,
                builder.getPostCallback("buildProductionTable", getArrayArgs(Operator.class)),
                Load.Operator
            );

            // Trivial error handler
            productions.addPatternMatch(Load.Operator, DumpType.errorHandler, noop);

            // Error handler with no callback but with closures
            productions.addPatternMatch(Load.Operator, DumpType.errorHandler, noop, Load.Closures);

            // Error handler with callback, no closures
            productions.addPatternMatch(
                Load.Operator, DumpType.errorHandler,
                builder.getPostCallback("parseErrorHandler", HostRoutine.class),
                Load.Method
            );

            // Error handler with callback and closures
            productions.addPatternMatch(
                Load.Operator, DumpType.errorHandler,
                builder.getPostCallback("parseErrorHandler", HostRoutine.class, getArrayArgs(Closure.class)),
                Load.Method, Load.Closures
            );

            // Non-leaf operator
            productions.addPatternMatch(
                Load.Operator, DumpType.operator,
                builder.getPostCallback("parseNonLeafOperator", TransitionPlane.class),
                Load.TransitionTable
            );

            // Leaf operator
            productions.addPatternMatch(
                Load.Operator, DumpType.operator,
                builder.getPostCallback("parseLeafOperator", TransitionTableLeaf.class),
                Load.LeafState
            );

            // Leaf transition table is a single predicated state
            productions.addPatternMatch(
                Load.LeafState,
                DumpType.leafState,
                passthrough,
                Load.TransitionTableLeaf
            );

            // Patterns
            productions.addVarArgsPatternMatch(Load.Patterns, DumpType.patterns, packageVarArgs, Load.Pattern);

            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcherLeafPost", HostRoutine.class),
                Load.PostCallback
            );
            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcherLeafPre", HostRoutine.class),
                Load.PreCallback
            );
            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcherLeafPreAndPost", HostRoutine.class, HostRoutine.class),
                Load.PreCallback, Load.PostCallback
            );
            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcherPost", Object.class, HostRoutine.class),
                Load.ChildTypes, Load.PostCallback
            );
            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcherPre", Object.class, HostRoutine.class),
                Load.ChildTypes, Load.PreCallback
            );
            productions.addPatternMatch(
                Load.Pattern, DumpType.pattern,
                builder.getPostCallback("parsePatternMatcher", Object.class, HostRoutine.class, HostRoutine.class),
                Load.ChildTypes, Load.PreCallback, Load.PostCallback
            );

            // State definition with no closures
            productions.addPatternMatch(
                Load.State, DumpType.state,
                builder.getPostCallback("parseState", getArrayArgs(PatternMatcher.class), getArrayArgs(CostEntry.class), getArrayArgs(HostRoutine.class)),
                Load.Patterns, Load.CostMap, Load.Predicates
            );

            // State definition with closures
            productions.addPatternMatch(
                Load.State, DumpType.state,
                builder.getPostCallback("parseState", getArrayArgs(PatternMatcher.class), getArrayArgs(CostEntry.class), getArrayArgs(Closure.class), getArrayArgs(HostRoutine.class)),
                Load.Patterns, Load.CostMap, Load.Closures, Load.Predicates
            );

            productions.addVarArgsPatternMatch(Load.ChildTypes, DumpType.childTypes, packageVarArgs, Load.ChildType);
            productions.addPatternMatch(Load.ChildType, DumpType.childType, builder.getPostCallback("parseNonterminal"));

            // Cost map
            productions.addVarArgsPatternMatch(Load.CostMap, DumpType.costMap, packageVarArgs, Load.Cost);
            productions.addPatternMatch(Load.Cost, DumpType.cost, builder.getPostCallback("parseCost"));

            // Closures
            productions.addVarArgsPatternMatch(Load.Closures, DumpType.closures, packageVarArgs, Load.Closure);
            productions.addPatternMatch(Load.Closure, DumpType.closure, builder.getPostCallback("parseClosureWithNoCallback"));
            productions.addPatternMatch(Load.Closure, DumpType.closure, builder.getPostCallback("parseClosureWithPost", HostRoutine.class), Load.PostCallback);
            productions.addPatternMatch(Load.Closure, DumpType.closure, builder.getPostCallback("parseClosureWithPre", HostRoutine.class), Load.PreCallback);
            productions.addPatternMatch(Load.Closure, DumpType.closure, builder.getPostCallback("parseClosureWithPreAndPost", HostRoutine.class, HostRoutine.class), Load.PreCallback, Load.PostCallback);

            // Callbacks
            productions.addPatternMatch(Load.PreCallback, DumpType.preCallback, passthrough, Load.Method);
            productions.addPatternMatch(Load.PostCallback, DumpType.postCallback, passthrough, Load.Method);

            // Method
            productions.addPatternMatch(Load.Method, DumpType.method, builder.getPostCallback("parseMethod", getArrayArgs(Class.class)), Load.ParameterTypes);
            productions.addVarArgsPatternMatch(Load.ParameterTypes, DumpType.parameterTypes, packageVarArgs, Load.Parameter);
            productions.addPatternMatch(Load.Parameter, DumpType.parameter, parseClass);

            // Predicates are possibly empty collections of methods
            productions.addVarArgsPatternMatch(Load.Predicates, DumpType.predicates, packageVarArgs, Load.Method);
            productions.addPatternMatch(Load.Predicates, DumpType.predicates, builder.getPostCallback("parseEmptyPredicates"));

            /*
             * Non-leaf transition tables
             */

            // Unary transition table
            productions.addPatternMatch(
                Load.TransitionTable, DumpType.transitionTable,
                passthrough,
                Load.FinalDimension
            );

            // Multi-dimensional transition table
            productions.addVarArgsPatternMatch(
                Load.TransitionTable, DumpType.transitionTable,
                passthrough,
                Load.NextDimension
            );

            // Final dimension
            productions.addVarArgsPatternMatch(
                Load.FinalDimension, DumpType.hyperPlane,
                builder.getPostCallback("createFinalDimension", Map.class, getArrayArgs(TransitionTableLeaf.class)),
                Load.FinalDimIndexMap, Load.TransitionTableLeaf
            );

            // Next dimension
            productions.addVarArgsPatternMatch(
                Load.NextDimension, DumpType.hyperPlane,
                builder.getPostCallback("createNextDimension", Map.class, getArrayArgs(TransitionPlane.class)),
                Load.NextDimIndexMap, Load.NextDimension
            );

            productions.addVarArgsPatternMatch(
                Load.NextDimension, DumpType.hyperPlane,
                builder.getPostCallback("createNextDimension", Map.class, getArrayArgs(TransitionPlane.class)),
                Load.NextDimIndexMap, Load.FinalDimension
            );

            // Index Maps are collections of state->index entries.
            productions.addVarArgsPatternMatch(
                Load.FinalDimIndexMap, DumpType.finalDimIndexMap,
                builder.getPostCallback("createIndexMap", getArrayArgs(IndexEntry.class)),
                Load.Index
            );

            productions.addVarArgsPatternMatch(
                Load.NextDimIndexMap, DumpType.nextDimIndexMap,
                builder.getPostCallback("createIndexMap", getArrayArgs(IndexEntry.class)),
                Load.Index
            );

            productions.addPatternMatch(
                Load.Index, DumpType.index,
                builder.getPostCallback("createIndexEntry")
            );

            // Predicated state
            productions.addVarArgsPatternMatch(
                Load.TransitionTableLeaf, DumpType.predicatedState, 
                builder.getPostCallback("parseTransitionTableLeaf", getArrayArgs(State.class)),
                Load.State
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

    class IndexEntry
    {
        Integer                                 stateNumber;
        Integer                                 index;

        IndexEntry(Integer stateNumber, Integer index)
        {
            this.stateNumber = stateNumber;
            this.index = index;
        }
    }

    // TODO: Encapsulate in an interface.
    public class TTBuilder
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

        public Class<?> parseClass(Node node)
        throws Exception
        {
            return Class.forName(node.getStringAttr("type"));
        }

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

        public HostRoutine parseMethod(Node node, Class<?>[] parameterTypes)
        throws Exception
        {
            Class<?> receiverClass = Class.forName(node.getStringAttr("class"));
            return HostRoutine.getHostRoutine(receiverClass.getDeclaredMethod(node.getStringAttr("name"),parameterTypes));
        }

        public Closure<Nonterminal> parseClosureWithPre(Node node, HostRoutine preCallback)
        {
            return parseClosureWithPreAndPost(node, preCallback, null);
        }

        public Closure<Nonterminal> parseClosureWithNoCallback(Node node)
        {
            return parseClosureWithPreAndPost(node, null, null);
        }

        public Closure<Nonterminal> parseClosureWithPost(Node node, HostRoutine postCallback)
        {
            return parseClosureWithPreAndPost(node, null, postCallback);
        }

        public Closure<Nonterminal> parseClosureWithPreAndPost(Node node, HostRoutine preCallback, HostRoutine postCallback)
        {
            return new Closure<Nonterminal>(
                getNonterminal(node.getStringAttr("nonterminal")),
                getNonterminal(node.getStringAttr("source")),
                node.getIntegerAttr("cost"),
                preCallback,
                postCallback
            );
        }

        public Nonterminal parseNonterminal(Node node)
        {
            return getNonterminal(node.getStringAttr("nonterminal"));
        }

        @SuppressWarnings("unchecked")
        public PatternMatcher<Nonterminal,NodeType> parsePatternMatcher(Node node, Object childTypes, HostRoutine preCallback, HostRoutine postCallback)
        {
            return new PatternMatcher<Nonterminal,NodeType>(
                getNonterminal(node.getStringAttr("nonterminal")),
                null,
                Integer.parseInt(node.getStringAttr("cost")),
                null,
                preCallback,
                postCallback,
                node.getBooleanAttr("variadic"), 
                Arrays.asList((Nonterminal[])childTypes)
            );
        }

        public PatternMatcher<Nonterminal,NodeType> parsePatternMatcherPost(Node node, Object childTypes, HostRoutine postCallback)
        {
            return parsePatternMatcher(node, childTypes, null, postCallback);
        }

        public PatternMatcher<Nonterminal,NodeType> parsePatternMatcherPre(Node node, Object childTypes, HostRoutine preCallback)
        {
            return parsePatternMatcher(node, childTypes, null, preCallback);
        }

        public PatternMatcher<Nonterminal,NodeType> parsePatternMatcherLeafPreAndPost(Node node, HostRoutine preCallback, HostRoutine postCallback)
        {
            return parsePatternMatcher(node, emptyNonterminals, preCallback, postCallback);
        }

        public PatternMatcher<Nonterminal,NodeType> parsePatternMatcherLeafPost(Node node, HostRoutine postCallback)
        {
            return parsePatternMatcher(node, emptyNonterminals, null, postCallback);
        }

        public PatternMatcher<Nonterminal,NodeType> parsePatternMatcherLeafPre(Node node, HostRoutine preCallback)
        {
            return parsePatternMatcher(node, emptyNonterminals, preCallback, null);
        }

        @SuppressWarnings("unchecked")
        public Object buildStateTable(Node node, State<Nonterminal, NodeType>... states)
        {
            this.stateTable = new ArrayList<State<Nonterminal, NodeType>>();
            for (State<Nonterminal, NodeType> s: states) {
                assert this.stateTable.size() == s.number: String.format("Expected state %d got state %d", this.stateTable.size(), s.number);
                this.stateTable.add(s);
            }

            return states;
        }
        
        @SafeVarargs
        public final Object buildProductionTable(Node node, Operator<Nonterminal, NodeType>... operatorTable)
        {
            for (Operator<Nonterminal, NodeType> op: operatorTable) {
                // TODO: Ugly hack for error handler; add semantics
                // to this visitor so it can use the semantics'
                // production-loading capabilities.
                if (op != null) {
                    this.productionTable.loadOperator(op);
                }
            }

            return this.productionTable;
        }

        public State<Nonterminal, NodeType> parseState(Node node, PatternMatcher<Nonterminal, NodeType>[] patternMatchers, CostEntry[] costs, Closure<Nonterminal>[] closures, HostRoutine[] predicates)
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

            if (predicates.length > 0) {

                for (HostRoutine m: predicates) {
                    result.predicates.add(m);
                }

                Collections.sort(result.predicates);
            }

            return result;
        }

        public IndexEntry createIndexEntry(Node node)
        {
            return new IndexEntry(Integer.valueOf(node.get("key")), Integer.valueOf(node.get("value")));
        }

        @SafeVarargs
        public final Map<Integer,Integer> createIndexMap(Node node, IndexEntry... entries)
        {
            Map<Integer,Integer> result = new HashMap<Integer,Integer>();

            for (IndexEntry entry: entries) {
                result.put(entry.stateNumber, entry.index);
            }

            return result;
        }

        // TODO: Maintain current dimension so loaded transition tables can generate hard-coded labelers.
        int currentDimension = -1;

        @SafeVarargs
        public final TransitionPlane<Nonterminal,NodeType> createNextDimension(Node node, Map<Integer,Integer> nextDimIndexMap, TransitionPlane<Nonterminal,NodeType>... nextDimension)
        {
            return new TransitionPlane<Nonterminal, NodeType>(productionTable, nextDimIndexMap, nextDimension, currentDimension);
        }

        @SafeVarargs
        public final TransitionPlane<Nonterminal,NodeType> createFinalDimension(Node node, Map<Integer,Integer> finalDimIndexMap, TransitionTableLeaf<Nonterminal, NodeType>... finalDimension)
        {
            return new TransitionPlane<Nonterminal,NodeType>(productionTable, finalDimIndexMap, finalDimension, currentDimension);
        }

        @SuppressWarnings("unchecked")
        public final State<Nonterminal, NodeType> parseState(Node node, PatternMatcher<Nonterminal, NodeType>[] patternMatchers, CostEntry[] costs, HostRoutine[] predicates)
        {
            return parseState(node, patternMatchers, costs, new Closure[0], predicates);
        }

        @SuppressWarnings("unchecked")
        public State<Nonterminal, NodeType> parseState(Node node, HostRoutine[] predicates)
        {
            return parseState(node, new PatternMatcher[0], null, new Closure[0], predicates);
        }

        public CostEntry parseCost(Node node)
        {
            return new CostEntry(getNonterminal(node.get("nonterminal")), Integer.parseInt(node.get("cost")));
        }

        public Object parseEmptyPredicates(Node node)
        {
            return new HostRoutine[0];
        }

        public Operator<Nonterminal, NodeType> parseLeafOperator(Node node, TransitionTableLeaf<Nonterminal, NodeType> predicatedState)
        {
            Operator<Nonterminal, NodeType> operator = new Operator<Nonterminal, NodeType>(getNodeType(node.get("nodeType")), 0, this.productionTable);
            operator.setLeafState(predicatedState);
            return operator;
        }

        public Operator<Nonterminal, NodeType> parseNonLeafOperator(Node node, TransitionPlane<Nonterminal, NodeType> rootPlane)
        {
            Operator<Nonterminal, NodeType> operator = new Operator<Nonterminal, NodeType>(getNodeType(node.get("nodeType")), Integer.parseInt(node.get("arity")), this.productionTable);
            operator.transitionTable = rootPlane;
            operator.setArityKind(node.getBooleanAttr("variadic")? ArityKind.Variadic:ArityKind.Fixed);
            return operator;
        }

        public Integer parseStateNumber(Node node)
        {
            return Integer.parseInt(node.get("stateNumber"));
        }

        @SafeVarargs
        public final Object parseTransitionTableLeaf(Node node, State<Nonterminal, NodeType>... states)
        {
            return new TransitionTableLeaf<Nonterminal, NodeType>(Arrays.asList(states));
        }

        @SuppressWarnings("unchecked")
        public Nonterminal getNonterminal(String ntName)
        {
            for (Object nt: nonterminalClass.getEnumConstants()) {
                if (nt.toString().equals(ntName)) {
                    return (Nonterminal)nt;
                }
            }

            throw new IllegalArgumentException(String.format("enumeration %s does not contain %s", nonterminalClass, ntName));
        }

        @SuppressWarnings("unchecked")
        public NodeType getNodeType(String ntName)
        {
            for (Object nt: nodeTypeClass.getEnumConstants()) {
                if (nt.toString().equals(ntName)) {
                    return (NodeType)nt;
                }
            }

            throw new IllegalArgumentException(String.format("enumeration %s does not contain %s", nodeTypeClass, ntName));
        }

        @SuppressWarnings("unchecked")
        public Object parseErrorHandler(Node node, HostRoutine callback)
        {
            return parseErrorHandler(node, callback, new Closure[0]);
        }

        public Object parseErrorHandler(Node node, HostRoutine callback, Closure<Nonterminal>[] closures)
        {
            Nonterminal errorNonterminal = getNonterminal(node.getStringAttr("nonterminal"));
            productionTable.addErrorHandler(errorNonterminal, callback);

            for (Closure<Nonterminal> c: closures) {
                productionTable.getErrorState().addClosure(c);
            }
            return null;
        }

        public HostRoutine getPostCallback(String methodName, Class<?>... formalTypes)
        {
            Class<?>[] formalsWithNode = new Class<?>[formalTypes.length+1];
            formalsWithNode[0] = Node.class;
            for (int i = 0; i < formalTypes.length; i++) {
                formalsWithNode[i+1] = formalTypes[i];
            }

            try {
                return HostRoutine.getHostRoutine(TTBuilder.class.getDeclaredMethod(methodName, formalsWithNode));
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new IllegalStateException(ex);
            }
        }

        public HostRoutine getPreCallback(String methodName)
        {
            Class<?>[] formals = { Node.class, Object.class };

            try {
                return HostRoutine.getHostRoutine(TTBuilder.class.getDeclaredMethod(methodName, formals));
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new IllegalStateException(ex);
            }
        }
    }
}
