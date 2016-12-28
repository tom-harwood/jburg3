import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import java.util.*;
import java.io.*;

/**
 * A NodeFactory builds testcases from an XML specification.
 */
class NodeFactory extends DefaultHandler
{
    Stack<Node> nodeStack = new Stack<Node>();

    List<Testcase> testcases = new ArrayList<Testcase>();

    public NodeFactory(String filename)
    throws Exception
    {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.parse(convertToFileURL(filename));
    }

    public static String convertToFileURL(String filename) {
        String path = new File(filename).getAbsolutePath();
        if (File.separatorChar != '/') {
            path = path.replace(File.separatorChar, '/');
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return "file:" + path;
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException
    {
        if (localName.equals("Test")) {
            // top-level node for grouping only at present

        } else if (localName.equals("Testcase")) {

            // Do a little validation to get better diagnostics.
            String testcaseName = atts.getValue("name");
            String testcaseType = atts.getValue("type");
            String canProduce = atts.getValue("canProduce");
            String cannotProduce = atts.getValue("cannotProduce");
            String expectedResult = atts.getValue("expected");
            String expectedException = atts.getValue("expectedException");

            if (canProduce != null) {
                testcases.add(new Testcase(testcaseName, Nonterminal.valueOf(canProduce), Testcase.TestType.CanProduce));
            } else if (cannotProduce != null) {
                testcases.add(new Testcase(testcaseName, Nonterminal.valueOf(cannotProduce), Testcase.TestType.CannotProduce));
            } else if (testcaseType == null) {
                throw new IllegalArgumentException(String.format("Testcase %s requires a type specifier", testcaseName));
            } else if (nodeStack.isEmpty()) {
                if (expectedException != null) {
                    testcases.add(new Testcase(testcaseName, Nonterminal.valueOf(testcaseType), expectedException, Testcase.TestType.Negative));
                } else {
                    testcases.add(new Testcase(testcaseName, Nonterminal.valueOf(testcaseType), expectedResult, Testcase.TestType.Normal));
                }
            } else {
                throw new IllegalStateException("Testcases cannot be nested.");
            }

        } else {
            Node        node;
            NodeType    nodeType = null;
            String      nodeTypeName = atts.getValue("op");
            String      content = atts.getValue("content");

            if (nodeTypeName != null) {
                nodeType = NodeType.valueOf(nodeTypeName);
            } else {
                throw new IllegalArgumentException("Nodes require an op specifier");
            }

            if (content != null) {
                node = new Node(nodeType, content);
            } else {
                node = new Node(nodeType);
            }

            if (nodeStack.isEmpty()) {

                if (!testcases.isEmpty()) {

                    Testcase currentTC = testcases.get(testcases.size()-1);
                    
                    if (currentTC.root == null) {
                        currentTC.root = node;
                    } else {
                        throw new IllegalStateException("Only one root node per Testcase.");
                    }

                } else {
                    throw new IllegalStateException("Node must be part of a Testcase.");
                }
            } else {
                nodeStack.peek().addChild(node);
            }

            nodeStack.push(node);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
    {
        if (localName.equals("Test")) {
        } else if (localName.equals("Testcase")) {
        } else {
            assert !nodeStack.isEmpty();
            nodeStack.pop();
        }
    }
}
