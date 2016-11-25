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
import jburg.semantics.BURMSemantics;
import jburg.semantics.HostRoutine;

/**
 * A XMLGrammar instance builds a ProductionTable from an XML specification.
 */
public class XMLGrammar<Nonterminal, NodeType> extends DefaultHandler
{
    Class<?> nonterminalClass;
    Class<?> nodeTypeClass;
    Class<?> reducerClass;
    Class<?> nodeClass;

    String  language = "java";

    Map<String,String> adHocNonterminals = new HashMap<String,String>();

    ProductionDesc currentProduction = null;

    BURMSemantics<Nonterminal> semantics = null;

    List<ProductionDesc> productions = new ArrayList<ProductionDesc>();

    /**
     * Set this to add productions to the table in a random order.
     */
    boolean randomizeProductions = false;

    public XMLGrammar(Class<?> nonterminalClass, Class<?> nodeTypeClass)
    {
        this.nonterminalClass = nonterminalClass;
        this.nodeTypeClass = nodeTypeClass;
    }

    public void setRandomized(boolean randomize)
    {
        this.randomizeProductions = randomize;
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
                pattern.nonterminal,
                pattern.nodeType,
                pattern.cost,
                pattern.predicate,
                pattern.preCallback,
                pattern.postCallback,
                pattern.isVarArgs,
                pattern.children
            );

        } else if (desc.isClosure()) {
            ClosureDesc closure = (ClosureDesc)desc;
            productionTable.addClosure(
                closure.nonterminal,
                closure.sourceNonterminal,
                closure.cost,
                closure.postCallback
            );

        } else if (desc.isErrorHandler()) {
            ErrorHandlerDesc errorHandler = (ErrorHandlerDesc)desc;
            productionTable.addErrorHandler(errorHandler.nonterminal, errorHandler.preCallback);

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

            // TODO: Read these from a .h file.
            nodeClass = getClass(localName, "nodeClass", atts);

            if (language.equals("java")) {
                reducerClass = getClass(localName, "reducerClass", atts);
            } else {
                // Dummy reducer class.
                // TODO: Have to abstract the method signatures;
                // that would be good for Java, too, so we don't
                // have to play javac.
                reducerClass = Object.class;
            }

        } else if (localName.equals("Pattern")) {
            startPattern(localName, atts);

        } else if (localName.equals("Semantics")) {
            startSemantics(localName, atts);

        } else if (localName.equals("Error")) {
            startErrorHandler(localName, atts);

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

        } else {
            throw new IllegalArgumentException("Unexpected " + localName);
        }
        } catch (NoSuchMethodException badCallback) {
            throw new IllegalArgumentException(badCallback);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
    {
        if (localName.equals("Pattern") || localName.equals("Closure") || localName.equals("Error")) {
            finishProduction();
        }
    }

    private Class<?> getClass(String localName, String attributeName, Attributes atts)
    {
        if (atts.getValue(attributeName) == null) {
            throw new IllegalArgumentException(String.format("element %s missing required class attribute %s", localName, attributeName));
        }

        try {
            return Class.forName(atts.getValue(attributeName));
        } catch (Exception ex) {
            throw new IllegalArgumentException("unable to get class " + atts.getValue(attributeName));
        }
    }

    @SuppressWarnings("unchecked")
    private void startSemantics(String localName, Attributes atts)
    {
        if (this.semantics == null) {
            try {
                this.semantics = new BURMSemantics<Nonterminal>(reducerClass, nodeClass, nonterminalClass);
            } catch (Exception nogood) {
                throw new IllegalArgumentException(nogood);
            }
        }
    }

    private void addNonterminal(String localName, Attributes atts)
    {
        String nonterminalValue = atts.getValue("nonterminal");
        if (this.semantics == null) {
            throw new IllegalStateException("Nonterminal elements must be inside Semantics elements");
        }

        if ("*".equals(nonterminalValue)) {
            this.semantics.setDefaultNonterminalClass(getClass(localName, "class", atts));
        } else if (language.equals("java")) {
            this.semantics.setNonterminalClass(getNonterminal(nonterminalValue), getClass(localName, "class", atts));
        } else {
            this.adHocNonterminals.put(nonterminalValue, atts.getValue("class"));
        }
    }

    private void startPattern(String localName, Attributes atts)
    {
        // This belongs in the constructor, but Java won't let it be there.
        boolean isVarArgs = atts.getValue("variadic") != null ? Boolean.parseBoolean(atts.getValue("variadic")): false;
        currentProduction = new PatternMatcherDesc(atts, isVarArgs);
    }

    private void startClosure(String localName, Attributes atts)
    {
        currentProduction = new ClosureDesc(atts);
    }

    private void startErrorHandler(String localName, Attributes atts)
    {
        currentProduction = new ErrorHandlerDesc(atts);
    }

    private void addChild(String localName, Attributes atts)
    {
        currentProduction.addChild(atts);
    }

    private void addPostCallback(String localName, Attributes atts)
    throws NoSuchMethodException
    {
        currentProduction.addPostCallback(atts);
    }

    private void addPreCallback(String localName, Attributes atts)
    throws NoSuchMethodException
    {
        currentProduction.addPreCallback(atts);
    }

    private void addPredicate(String localName, Attributes atts)
    throws NoSuchMethodException
    {
        currentProduction.addPredicate(atts);
    }

    private void finishProduction()
    {
        productions.add(currentProduction);
        currentProduction = null;
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

    /**
     * Build-time description of a pattern matcher or closure.
     */
    private abstract class ProductionDesc
    {
        final Nonterminal           nonterminal;
        final List<Nonterminal>     children = new ArrayList<Nonterminal>();
        final int                   cost;
        final boolean               isVarArgs;

        HostRoutine predicate   = null;
        HostRoutine preCallback = null;
        HostRoutine postCallback = null;

        abstract boolean isPatternMatch();
        abstract boolean isClosure();
        abstract boolean isErrorHandler();
        abstract boolean isNullHandler();

        ProductionDesc(Attributes atts, boolean isVarArgs)
        {
            this.nonterminal    = getNonterminal(atts.getValue("nonterminal"));
            this.cost           = atts.getValue("cost") != null ? Integer.parseInt(atts.getValue("cost")): 1;
            this.isVarArgs      = isVarArgs;
        }

        void addChild(Attributes atts)
        {
            children.add(getNonterminal(atts.getValue("nonterminal")));
        }

        void addPostCallback(Attributes atts)
        throws NoSuchMethodException
        {
            if (this.postCallback == null) {
                this.postCallback = getPostCallbackMethod(atts);
            } else {
                throw new IllegalArgumentException("postCallback can only be specified once.");
            }
        }

        void addPreCallback(Attributes atts)
        throws NoSuchMethodException
        {
            if (this.preCallback == null) {
                this.preCallback = getPreCallbackMethod(atts);
            } else {
                throw new IllegalArgumentException("preCallback can only be specified once.");
            }
        }

        void addPredicate(Attributes atts)
        throws NoSuchMethodException
        {
            if (this.predicate == null) {
                this.predicate = getPredicateMethod(atts);
            } else {
                throw new IllegalArgumentException("predicate can only be specified once.");
            }
        }

        @SuppressWarnings("unchecked")
        HostRoutine getPostCallbackMethod(Attributes atts)
        throws NoSuchMethodException
        {
            checkSemantics();
            String methodName = atts.getValue("name");

            if (isClosure()) {
                return semantics.getPostCallback(methodName, false, this.nonterminal, ((ClosureDesc)this).sourceNonterminal);
            } else {
                return semantics.getPostCallback(methodName, this.isVarArgs, this.nonterminal, (Nonterminal[])this.children.toArray());
            }
        }

        HostRoutine getPreCallbackMethod(Attributes atts)
        throws NoSuchMethodException
        {
            checkSemantics();
            String methodName = atts.getValue("name");
            return semantics.getPreCallback(methodName);
        }

        HostRoutine getPredicateMethod(Attributes atts)
        throws NoSuchMethodException
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
        final NodeType  nodeType;

        PatternMatcherDesc(Attributes atts, boolean isVarArgs)
        {
            super(atts, isVarArgs);
            this.nodeType   = getNodeType(atts.getValue("nodeType"));
        }

        boolean isPatternMatch()    { return true; }
        boolean isClosure()         { return false; }
        boolean isErrorHandler()    { return false; }
        boolean isNullHandler()     { return false; }
    }

    private class ClosureDesc extends ProductionDesc
    {
        final Nonterminal           sourceNonterminal;

        ClosureDesc(Attributes atts)
        {
            super(atts, false);
            this.sourceNonterminal = getNonterminal(atts.getValue("sourceNonterminal"));
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
