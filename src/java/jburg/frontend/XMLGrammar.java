package jburg.frontend;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import jburg.ProductionTable;
import jburg.BURMSemantics;

/**
 * A XMLGrammar instance builds a ProductionTable from an XML specification.
 */
public class XMLGrammar<Nonterminal, NodeType> extends DefaultHandler
{
    Class<?> nonterminalClass;
    Class<?> nodeTypeClass;
    Class<?> reducerClass;
    Class<?> nodeClass;

    ProductionDesc currentPattern = null;

    BURMSemantics<Nonterminal> semantics = null;

    List<ProductionDesc> productions = new ArrayList<ProductionDesc>();

    /**
     * Set this to add productions to the table in a random order.
     */
    boolean randomizeProductions = false;

    enum PatternPrecondition{ Present, Absent }

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
            productionTable.addPatternMatch(
                desc.nonterminal,
                desc.nodeType,
                desc.cost,
                desc.predicate,
                desc.preCallback,
                desc.postCallback,
                desc.isVarArgs,
                desc.children
            );
        } else {
            productionTable.addClosure(
                desc.nonterminal,
                desc.sourceNonterminal,
                desc.cost,
                desc.postCallback
            );
        }
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException
    {
        try {
        if (localName.equals("Grammar")) {
            reducerClass = getClass(localName, "reducerClass", atts);
            nodeClass = getClass(localName, "nodeClass", atts);

        } else if (localName.equals("Pattern")) {
            startPattern(localName, atts);

        } else if (localName.equals("Semantics")) {
            startSemantics(localName, atts);

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
        if (localName.equals("Pattern") || localName.equals("Closure")) {
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
        if (this.semantics == null) {
            throw new IllegalStateException("Nonterminal elements must be inside Semantics elements");
        }

        this.semantics.setNonterminalClass(getNonterminal(atts.getValue("nonterminal")), getClass(localName, "class", atts));
    }

    private void startPattern(String localName, Attributes atts)
    {
        checkPatternState(localName, PatternPrecondition.Absent);
        currentPattern = new ProductionDesc(atts);
    }

    private void finishProduction()
    {
        productions.add(currentPattern);
        currentPattern = null;
    }

    private void startClosure(String localName, Attributes atts)
    {
        checkPatternState(localName, PatternPrecondition.Absent);
        currentPattern = new ProductionDesc(atts, getNonterminal(atts.getValue("sourceNonterminal")));
    }

    private void addChild(String localName, Attributes atts)
    {
        checkPatternState(localName, PatternPrecondition.Present);
        currentPattern.addChild(atts);
    }

    private void addPostCallback(String localName, Attributes atts)
    throws NoSuchMethodException
    {
        checkPatternState(localName, PatternPrecondition.Present);
        currentPattern.addPostCallback(atts);
    }

    private void addPreCallback(String localName, Attributes atts)
    throws NoSuchMethodException
    {
        checkPatternState(localName, PatternPrecondition.Present);
        currentPattern.addPreCallback(atts);
    }

    private void addPredicate(String localName, Attributes atts)
    throws NoSuchMethodException
    {
        checkPatternState(localName, PatternPrecondition.Present);
        currentPattern.addPredicate(atts);
    }

    private void checkPatternState(String localName, PatternPrecondition precondition)
    {
        if (precondition == PatternPrecondition.Present && currentPattern == null) {
            throw new IllegalStateException(localName + " element requires a Pattern parent");
        } else if (precondition == PatternPrecondition.Absent && currentPattern != null) {
            throw new IllegalStateException(localName + " elements cannot have a Pattern ancestor");
        }
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
    private class ProductionDesc
    {
        final Nonterminal           nonterminal;
        final Nonterminal           sourceNonterminal;
        final NodeType              nodeType;
        final List<Nonterminal>     children = new ArrayList<Nonterminal>();
        final int                   cost;
        final boolean               isVarArgs;

        final static int            ANY_ARITY = -1;

        Method predicate   = null;
        Method preCallback = null;
        Method postCallback = null;

        ProductionDesc(Attributes atts)
        {
            this.nonterminal        = getNonterminal(atts.getValue("nonterminal"));
            this.sourceNonterminal  = null;
            this.nodeType           = getNodeType(atts.getValue("nodeType"));
            this.cost               = atts.getValue("cost") != null ? Integer.parseInt(atts.getValue("cost")): 1;
            this.isVarArgs          = atts.getValue("variadic") != null ? Boolean.parseBoolean(atts.getValue("variadic")): false;
        }

        ProductionDesc(Attributes atts, Nonterminal sourceNonterminal)
        {
            this.nonterminal        = getNonterminal(atts.getValue("nonterminal"));
            this.sourceNonterminal  = sourceNonterminal;
            this.nodeType           = null;
            this.cost               = atts.getValue("cost") != null ? Integer.parseInt(atts.getValue("cost")): 1;
            this.isVarArgs          = false;
        }

        void addChild(Attributes atts)
        {
            children.add(getNonterminal(atts.getValue("nonterminal")));
        }

        boolean isPatternMatch()
        {
            assert(nodeType != null || sourceNonterminal != null);
            return nodeType != null;
        }

        void addPostCallback(Attributes atts)
        throws NoSuchMethodException
        {
            this.postCallback = getPostCallbackMethod(atts);
        }

        void addPreCallback(Attributes atts)
        throws NoSuchMethodException
        {
            this.preCallback = getPreCallbackMethod(atts);
        }

        void addPredicate(Attributes atts)
        throws NoSuchMethodException
        {
            this.predicate = getPredicateMethod(atts);
        }

        @SuppressWarnings("unchecked")
        Method getPostCallbackMethod(Attributes atts)
        throws NoSuchMethodException
        {
            checkSemantics();
            String methodName = atts.getValue("name");

            if (isPatternMatch()) {
                return semantics.getPostCallback(methodName, this.isVarArgs, this.nonterminal, (Nonterminal[])this.children.toArray());
            } else {
                return semantics.getPostCallback(methodName, false, this.nonterminal, this.sourceNonterminal);
            }
        }

        Method getPreCallbackMethod(Attributes atts)
        throws NoSuchMethodException
        {
            checkSemantics();
            String methodName = atts.getValue("name");
            return semantics.getPreCallback(methodName);
        }

        Method getPredicateMethod(Attributes atts)
        throws NoSuchMethodException
        {
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
}
