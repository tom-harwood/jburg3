package jburg.frontend;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import jburg.ProductionTable;
import jburg.semantics.*;

/**
 * A XMLGrammar instance builds a ProductionTable from an XML specification.
 */
public class XMLGrammar<Nonterminal, NodeType> extends DefaultHandler
{
    String nonterminalClass;
    String nodeTypeClass;
    String reducerClassName;
    String nodeClassName;

    String  language = "java";

    String verboseTrigger = null;

    Stack<ProductionDesc> activeProductions = new Stack<ProductionDesc>();

    BURMSemantics<Nonterminal,NodeType> semantics = null;

    List<ProductionDesc> productions = new ArrayList<ProductionDesc>();

    /**
     * Set this to add productions to the table in a random order.
     */
    boolean randomizeProductions = false;

    public XMLGrammar(String nonterminalClass, String nodeTypeClass)
    {
        this(nonterminalClass, nodeTypeClass, null);
    }

    public XMLGrammar(String nonterminalClass, String nodeTypeClass, BURMSemantics<Nonterminal,NodeType> semantics)
    {
        this.nonterminalClass = nonterminalClass;
        this.nodeTypeClass = nodeTypeClass;
        this.semantics = semantics;
    }

    public void setRandomized(boolean randomize)
    {
        this.randomizeProductions = randomize;
    }

    public void setVerboseTrigger(String trigger)
    {
        this.verboseTrigger = trigger;
    }

    public BURMSemantics<?,?> getSemantics()
    {
        return this.semantics;
    }

    public ProductionTable<Nonterminal,NodeType> build(String filename)
    throws Exception
    {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(getClass().getClassLoader().getResource("xsd/JBurg3.xsd"));
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(filename));

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.parse(filename);

        ProductionTable<Nonterminal,NodeType> result = new ProductionTable<Nonterminal,NodeType>();
        result.setVerboseTrigger(verboseTrigger);

        if (!randomizeProductions) {

            for (ProductionDesc desc: productions) {
                addProduction(desc, result);
            }
        } else {

            // "bogo-unsort" the productions.
            Set<Integer> finishedProductions = new HashSet<Integer>();
            Random prng = new Random();

            while (finishedProductions.size() != productions.size()) {
                int index = prng.nextInt(productions.size());

                if (finishedProductions.add(index)) {
                    addProduction(productions.get(index), result);
                }
            }
        }

        result.generateStates();
        return result;
    }

    private void addProduction(ProductionDesc desc, ProductionTable<Nonterminal,NodeType> productionTable)
    {
        if (desc.isPatternMatch()) {
            PatternMatcherDesc pattern = (PatternMatcherDesc)desc;

            productionTable.addPatternMatch(
                semantics.getNonterminal(pattern.nonterminal),
                semantics.getNodeType(pattern.nodeType),
                pattern.cost,
                pattern.predicate,
                pattern.preCallback,
                pattern.postCallback,
                pattern.isVarArgs,
                pattern.getNonterminals()
            );

        } else if (desc.isClosure()) {
            ClosureDesc closure = (ClosureDesc)desc;
            productionTable.addClosure(
                semantics.getNonterminal(closure.nonterminal),
                semantics.getNonterminal(closure.sourceNonterminal),
                closure.cost,
                closure.postCallback
            );

        } else if (desc.isErrorHandler()) {
            ErrorHandlerDesc errorHandler = (ErrorHandlerDesc)desc;
            productionTable.addErrorHandler(semantics.getNonterminal(errorHandler.nonterminal), errorHandler.preCallback);

        } else if (desc.isNullHandler()) {
            NullHandlerDesc nullHandler = (NullHandlerDesc)desc;
            productionTable.addNullPointerProduction(semantics.getNonterminal(nullHandler.nonterminal), 1, nullHandler.preCallback);

        } else {
            throw new IllegalStateException(String.format("Unhandled production type %s", desc.getClass()));
        }
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException
    {
        try {
            if (localName.equals("Grammar")) {
                if (atts.getValue("language") != null) {
                    this.language = atts.getValue("language");
                }

                nodeClassName = getClassName(localName, "nodeClass", atts);
                reducerClassName = getClassName(localName, "reducerClass", atts);

            } else if (localName.equals("Pattern")) {
                startPattern(localName, atts);

            } else if (localName.equals("Semantics")) {
                startSemantics(localName, atts);

            } else if (localName.equals("Error")) {
                startErrorHandler(localName, atts);

            } else if (localName.equals("NullNode")) {
                startNullHandler(localName, atts);

            } else if (localName.equals("Nonterminal")) {
                addNonterminal(localName, atts);

            } else if (localName.equals("Closure")) {
                startClosure(localName, atts);

            } else if (localName.equals("child")) {
                addChild(localName, atts);

            } else if (localName.equals("postCallback")) {
                addPostCallback(localName, atts);

            } else if (localName.equals("preCallback")) {
                addPreCallback(localName, atts);

            } else if (localName.equals("predicate")) {
                addPredicate(localName, atts);

            } else if (localName.equals("errorHandler")) {
                addPreCallback(localName, atts);

            } else if (localName.equals("nullHandler")) {
                addPreCallback(localName, atts);

            } else {
                throw new IllegalArgumentException("Unexpected " + localName);
            }
        } catch (Exception badCallback) {
            badCallback.printStackTrace();
            throw new IllegalArgumentException(badCallback);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
    {
        if (localName.equals("Pattern") || localName.equals("Closure") || localName.equals("Error") || localName.equals("NullNode")) {
            finishProduction();
        }
    }

    private String getClassName(String localName, String attributeName, Attributes atts)
    {
        if (atts.getValue(attributeName) == null) {
            throw new IllegalArgumentException(String.format("element %s missing required class attribute %s", localName, attributeName));
        }

        return atts.getValue(attributeName);
    }

    @SuppressWarnings("unchecked")
    private void startSemantics(String localName, Attributes atts)
    {
        if (this.semantics == null) {

            try {
                if ("java".equals(this.language)) {
                    this.semantics = new JavaSemantics<Nonterminal,NodeType>(reducerClassName, nodeClassName, nodeTypeClass, nonterminalClass);

                } else if ("cpp".equals(this.language) || "C++".equalsIgnoreCase(this.language)) {
                    this.semantics = new CppSemantics<Nonterminal,NodeType>(nodeClassName, reducerClassName);
                }
            } catch (Exception nogood) {
                throw new IllegalArgumentException(nogood);
            }
        }
    }

    private void addNonterminal(String localName, Attributes atts)
    {
        // XML grammar should disallow this.
        assert (this.semantics != null);
        String nonterminalValue = atts.getValue("nonterminal");

        if ("*".equals(nonterminalValue)) {
            this.semantics.setDefaultNonterminalClass(getClassName(localName, "class", atts));
        } else {
            this.semantics.setNonterminalClass(nonterminalValue, getClassName(localName, "class", atts));
        }
    }

    private void startPattern(String localName, Attributes atts)
    {
        // This belongs in the constructor, but Java won't let it be there.
        boolean isVarArgs = atts.getValue("variadic") != null ? Boolean.parseBoolean(atts.getValue("variadic")): false;
        activeProductions.push(new PatternMatcherDesc(atts, isVarArgs));
    }

    private void startClosure(String localName, Attributes atts)
    {
        activeProductions.push(new ClosureDesc(atts));
    }

    private void startErrorHandler(String localName, Attributes atts)
    {
        activeProductions.push(new ErrorHandlerDesc(atts));
    }

    private void startNullHandler(String localName, Attributes atts)
    {
        activeProductions.push(new NullHandlerDesc(atts));
    }

    private void addChild(String localName, Attributes atts)
    {
        getCurrentProduction().addChild(atts);
    }

    private void addPostCallback(String localName, Attributes atts)
    throws Exception
    {
        getCurrentProduction().addPostCallback(atts);
    }

    private void addPreCallback(String localName, Attributes atts)
    throws Exception
    {
        getCurrentProduction().addPreCallback(atts);
    }

    private void addPredicate(String localName, Attributes atts)
    throws Exception
    {
        getCurrentProduction().addPredicate(atts);
    }

    private void finishProduction()
    {
        ProductionDesc newProduction = getCurrentProduction();
        productions.add(newProduction);
        activeProductions.pop();

        // Nested production?
        if (!activeProductions.isEmpty()) {
            // Create a synthetic nonterminal so this nested production only matches in its specific position.
            // The specification XSD grammar validates that the parent and child are both patterns.
            PatternMatcherDesc parentPattern = (PatternMatcherDesc)getCurrentProduction();
            PatternMatcherDesc childPattern = (PatternMatcherDesc)newProduction;
            String originalNonterminal = newProduction.nonterminal;
            Object originalMapping = semantics.getNonterminalMapping(originalNonterminal);

            newProduction.nonterminal = String.format(
                "%s_%s_%x_child_%d_%s",
                originalNonterminal,
                parentPattern.nodeType,
                System.identityHashCode(parentPattern),
                parentPattern.children.size(),
                childPattern.nodeType
            );

            // Establish a mapping from the synthetic nonterminal
            // to the original nonterminal's result class.
            semantics.setNonterminalClass(newProduction.nonterminal, originalMapping);
            // Make the synthetic nonterminal the parent pattern's nonterminal goal
            // for the child pattern's position.
            parentPattern.children.add(newProduction.nonterminal);
        }
    }

    ProductionDesc getCurrentProduction()
    {
        if (activeProductions.isEmpty()) {
            throw new IllegalStateException("No active production.");
        } else {
            return activeProductions.peek();
        }
    }


    /**
     * Build-time description of a pattern matcher or closure.
     */
    private abstract class ProductionDesc
    {
        String              nonterminal;
        final List<String>  children = new ArrayList<String>();
        final int           cost;
        final boolean       isVarArgs;

        HostRoutine predicate   = null;
        HostRoutine preCallback = null;
        HostRoutine postCallback = null;

        abstract boolean isPatternMatch();
        abstract boolean isClosure();
        abstract boolean isErrorHandler();
        abstract boolean isNullHandler();

        ProductionDesc(Attributes atts, boolean isVarArgs)
        {
            this.nonterminal    = atts.getValue("nonterminal");
            this.cost           = atts.getValue("cost") != null ? Integer.parseInt(atts.getValue("cost")): 1;
            this.isVarArgs      = isVarArgs;
        }

        void addChild(Attributes atts)
        {
            children.add(atts.getValue("nonterminal"));
        }

        void addPostCallback(Attributes atts)
        throws Exception
        {
            if (this.postCallback == null) {
                this.postCallback = getPostCallbackMethod(atts);
            } else {
                throw new IllegalArgumentException("postCallback can only be specified once.");
            }
        }

        void addPreCallback(Attributes atts)
        throws Exception
        {
            if (this.preCallback == null) {
                this.preCallback = getPreCallbackMethod(atts);
            } else {
                throw new IllegalArgumentException("preCallback can only be specified once.");
            }
        }

        void addPredicate(Attributes atts)
        throws Exception
        {
            if (this.predicate == null) {
                this.predicate = getPredicateMethod(atts);
            } else {
                throw new IllegalArgumentException("predicate can only be specified once.");
            }
        }

        @SuppressWarnings("unchecked")
        HostRoutine getPostCallbackMethod(Attributes atts)
        throws Exception
        {
            checkSemantics();
            String methodName = atts.getValue("name");

            if (isClosure()) {
                return semantics.getPostCallback(methodName, false, this.nonterminal, semantics.getNonterminal(((ClosureDesc)this).sourceNonterminal));
            } else {
                return semantics.getPostCallback(methodName, this.isVarArgs, semantics.getNonterminal(this.nonterminal), getChildNonterminals());
            }
        }

        private Object[] getChildNonterminals()
        {
            Object[] result = new Object[children.size()];

            for (int i = 0; i < children.size(); i++) {
                result[i] = semantics.getNonterminal(children.get(i));
            }

            return result;
        }

        HostRoutine getPreCallbackMethod(Attributes atts)
        throws Exception
        {
            checkSemantics();
            String methodName = atts.getValue("name");
            return semantics.getPreCallback(methodName);
        }

        HostRoutine getPredicateMethod(Attributes atts)
        throws Exception
        {
            checkSemantics();
            String methodName = atts.getValue("name");
            return semantics.getPredicate(methodName);
        }

        void checkSemantics()
        {
            if (XMLGrammar.this.semantics == null) {
                throw new IllegalStateException("Specify <Semantics> before rules");
            }
        }

    }

    private class PatternMatcherDesc extends ProductionDesc
    {
        final String    nodeType;
        final boolean   childrenFinalized;

        PatternMatcherDesc(Attributes atts, boolean isVarArgs)
        {
            super(atts, isVarArgs);
            this.nodeType   = atts.getValue("nodeType");

            String onlyChildNonterminal = atts.getValue("onlyChildNonterminal");

            if (onlyChildNonterminal != null) {
                children.add(onlyChildNonterminal);
                childrenFinalized = true;
            } else {
                childrenFinalized = false;
            }
        }

        boolean isPatternMatch()    { return true; }
        boolean isClosure()         { return false; }
        boolean isErrorHandler()    { return false; }
        boolean isNullHandler()     { return false; }

        @Override
        void addChild(Attributes atts)
        {
            if (!childrenFinalized) {
                super.addChild(atts);
            } else {
                throw new IllegalArgumentException("A Pattern with an onlyChildNonterminal attribute cannot have child nodes.");
            }
        }

        List<Object> getNonterminals()
        {
            List<Object> result = new ArrayList<Object>();
            for (int i = 0; i < this.children.size(); i++) {
                result.add(semantics.getNonterminal(this.children.get(i)));
            }

            return result;
        }

    }

    private class ClosureDesc extends ProductionDesc
    {
        final String    sourceNonterminal;

        ClosureDesc(Attributes atts)
        {
            super(atts, false);
            this.sourceNonterminal = atts.getValue("sourceNonterminal");
        }

        boolean isPatternMatch()    { return false; }
        boolean isClosure()         { return true; }
        boolean isErrorHandler()    { return false; }
        boolean isNullHandler()     { return false; }
    }

    private class ErrorHandlerDesc extends ProductionDesc
    {
        ErrorHandlerDesc(Attributes atts)
        {
            super(atts, false);
        }

        boolean isClosure()         { return false; }
        boolean isPatternMatch()    { return false; }
        boolean isErrorHandler()    { return true; }
        boolean isNullHandler()     { return false; }
    }

    private class NullHandlerDesc extends ProductionDesc
    {
        NullHandlerDesc(Attributes atts)
        {
            super(atts, false);
        }

        boolean isClosure()         { return false; }
        boolean isPatternMatch()    { return false; }
        boolean isErrorHandler()    { return false; }
        boolean isNullHandler()     { return true; }
    }
}
