package jburg.util;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * PrintState prints details of a state useful for debugging.
 */
public class PrintState
{
    static final String outputEncoding = "UTF-8";
    final static XPathFactory factory = XPathFactory.newInstance();

    public static void main(String[] args)
    throws Exception
    {
        File   xmlFile  = new File(args[0]);
        String stateNum = args[1];

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setCoalescing(true);
        dbf.setExpandEntityReferences(false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(xmlFile);

        for (Finding finding: analyzeState(doc, stateNum)) {
            System.out.println(finding.toString());
        }
    }

    public static List<Finding> analyzeState(Document doc, String stateNum)
    throws Exception
    {
        List<Finding> uniqueFindings = new ArrayList<Finding>();

        for (Node node: findNodes(doc, String.format("//state[@number=%s]", stateNum))) {

            for (Node pattern: findNodes(node, ".//pattern")) {
                    Finding finding = new PatternFinding(pattern);

                if (!uniqueFindings.contains(finding)) {
                    uniqueFindings.add(finding);
                }
            }

            // TODO: These would be more useful with a topological
            // sort so dependencies came out in order.
            for (Node closure: findNodes(node, ".//closure")) {
                Finding finding = new ClosureFinding(getNamedAttribute(closure, "nonterminal"), getNamedAttribute(closure, "source"));

                if (!uniqueFindings.contains(finding)) {
                    uniqueFindings.add(finding);
                }
            }
        }

        return uniqueFindings;
    }

    static String getNamedAttribute(Node node, String attrName)
    {
        Node nonterminal = node.getAttributes().getNamedItem(attrName);
        if (nonterminal != null) {
            return nonterminal.getNodeValue();
        } else {
            throw new IllegalStateException(String.format("No attribute \"%s\"", attrName));
        }
    }

    static Iterable<Node> findNodes(Object item, String pattern)
    throws Exception
    {
        NodeList nodes = (NodeList)factory.newXPath().compile(pattern).evaluate(item, XPathConstants.NODESET);
        int n = nodes.getLength();
        List<Node> result = new ArrayList<Node>(n);

        for (int i = 0; i < n; i++) {
            result.add(nodes.item(i));
        }

        return result;
    }

    public static abstract class Finding
    {
        public abstract String getNonterminal();
        public abstract String getSourceNonterminal();
        public abstract boolean hasSourceNonterminal();
    }

    public static class PatternFinding extends Finding
    {
        public final String nonterminal;
        public final String nodeType;
        public final List<String> childNonterminals = new ArrayList<String>();

        PatternFinding(Node pattern)
        {
            this.nonterminal = getNamedAttribute(pattern, "nonterminal");
            this.nodeType = getNamedAttribute(pattern, "nodeType");

            try {
                for (Node child: findNodes(pattern, ".//childType")) {
                    childNonterminals.add(getNamedAttribute(child, "nonterminal"));
                }
            } catch (Exception cantGetChildren) {
                childNonterminals.add(String.format("<error getting children:%s",cantGetChildren));
            }
        }

        public String getNonterminal()
        {
            return this.nonterminal;
        }

        public String getSourceNonterminal()
        {
            throw new UnsupportedOperationException();
        }

        public boolean hasSourceNonterminal()
        {
            return false;
        }

        public String toString()
        {
            if (childNonterminals.size() > 0) {
                return String.format("pattern %s%s => %s", nodeType, childNonterminals, nonterminal);
            } else {
                return String.format("pattern %s => %s", nodeType, nonterminal);
            }
        }

        public int hashCode()
        {
            return nonterminal.hashCode() * 31 + childNonterminals.hashCode();
        }

        public boolean equals(Object o)
        {
            if (o instanceof PatternFinding) {
                PatternFinding p = (PatternFinding)o;
                return this.nonterminal.equals(p.nonterminal) && this.childNonterminals.equals(p.childNonterminals);
            } else {
                return false;
            }
        }
    }

    public static class ClosureFinding extends Finding
    {
        public final String nonterminal;
        public final String sourceNonterminal;

        ClosureFinding(String nonterminal, String sourceNonterminal)
        {
            this.nonterminal = nonterminal;
            this.sourceNonterminal = sourceNonterminal;
        }

        public String getNonterminal()
        {
            return this.nonterminal;
        }

        public String getSourceNonterminal()
        {
            return this.sourceNonterminal;
        }

        public boolean hasSourceNonterminal()
        {
            return true;
        }

        public String toString()
        {
            return sourceNonterminal + " => " + nonterminal;
        }

        public int hashCode()
        {
            return nonterminal.hashCode() * 31 + sourceNonterminal.hashCode();
        }

        public boolean equals(Object o)
        {
            if (o instanceof ClosureFinding) {
                ClosureFinding c = (ClosureFinding)o;
                return this.nonterminal.equals(c.nonterminal) && this.sourceNonterminal.equals(c.sourceNonterminal);
            } else {
                return false;
            }
        }
    }
}
