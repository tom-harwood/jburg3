import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import jburg.ProductionTable;

/**
 * A GrammarBuilder builds a BURM from an XML specification.
 */
class GrammarBuilder<Nonterminal, NodeType> extends DefaultHandler
{
    Class<?> nonterminalClass;
    Class<?> nodeTypeClass;
    Class<?> reducerClass;
    Class<?> nodeClass;

    ProductionTable<Nonterminal, NodeType> productions = new ProductionTable<Nonterminal,NodeType>();

    PatternDesc currentPattern = null;

    enum PatternPrecondition{ Present, Absent }

    public GrammarBuilder(Class<?> nonterminalClass, Class<?> nodeTypeClass)
    {
        this.nonterminalClass = nonterminalClass;
        this.nodeTypeClass = nodeTypeClass;
    }

    ProductionTable<Nonterminal,NodeType> build(String filename)
    throws Exception
    {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.parse(filename);

        if (productions != null) {
            return productions;
        } else {
            throw new IllegalStateException("no production table generated.");
        }
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException
    {
        if (localName.equals("Grammar")) {
            reducerClass = getClass(localName, "reducerClass", atts);
            nodeClass = getClass(localName, "nodeClass", atts);
        } else if (localName.equals("Pattern")) {
            startPattern(localName, atts);
        } else if (localName.equals("child")) {
            addChild(localName, atts);
        } else if (localName.equals("postCallback")) {
            addPostCallback(localName, atts);
        } else {
            throw new IllegalArgumentException("Unexpected " + localName);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
    {
        if (localName.equals("Grammar")) {
            generateProductions();
        } else if (localName.equals("Pattern")) {
            finishPattern();
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

    private void generateProductions()
    {
        productions.generateStates();
    }

    private void startPattern(String localName, Attributes atts)
    {
        checkPatternState(localName, PatternPrecondition.Absent);
        currentPattern = new PatternDesc(atts);
    }

    private void finishPattern()
    {
        productions.addPatternMatch(
            currentPattern.nonterminal,
            currentPattern.nodeType,
            currentPattern.cost,
            currentPattern.predicate,
            currentPattern.preCallback,
            currentPattern.postCallback,
            currentPattern.isVarArgs,
            currentPattern.children
        );

        currentPattern = null;
    }

    private void addChild(String localName, Attributes atts)
    {
        checkPatternState(localName, PatternPrecondition.Present);
        currentPattern.addChild(atts);
    }

    private void addPostCallback(String localName, Attributes atts)
    {
        checkPatternState(localName, PatternPrecondition.Present);
        currentPattern.addPostCallback(atts);
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

    private class PatternDesc
    {
        final Nonterminal           nonterminal;
        final NodeType              nodeType;
        final List<Nonterminal>     children = new ArrayList<Nonterminal>();
        final int                   cost;
        final boolean               isVarArgs;

        Method predicate   = null;
        Method preCallback = null;
        Method postCallback = null;

        PatternDesc(Attributes atts)
        {
            this.nonterminal    = getNonterminal(atts.getValue("nonterminal"));
            this.nodeType       = getNodeType(atts.getValue("nodeType"));
            this.cost           = atts.getValue("cost") != null ? Integer.parseInt(atts.getValue("cost")): 1;
            this.isVarArgs      = atts.getValue("variadic") != null ? Boolean.parseBoolean(atts.getValue("variadic")): false;
        }

        void addChild(Attributes atts)
        {
            children.add(getNonterminal(atts.getValue("nonterminal")));
        }

        void addPostCallback(Attributes atts)
        {
            this.postCallback = getCallbackMethod(atts.getValue("name"));
        }

        Method getCallbackMethod(String methodName)
        {
            Method candidate = null;

            for (Method m: reducerClass.getDeclaredMethods()) {

                if (m.getName().equals(methodName)) {
                    
                    Class<?>[] parameterTypes = m.getParameterTypes();

                    if (parameterTypes.length == children.size() + 1 && parameterTypes[0].equals(nodeClass)) {
                        
                        if (candidate == null) {
                            candidate = m;
                        } else {
                            throw new IllegalArgumentException(String.format("unable to disambiguate %s and %s", candidate, m));
                        }
                    }
                }
            }

            if (candidate != null) {
                return candidate;
            } else {
                throw new IllegalArgumentException(String.format("No callback method %s(%s, %s)", methodName, nodeClass.getName(), children));
            }
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
    }
}
