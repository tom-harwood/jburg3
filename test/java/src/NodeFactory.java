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

    private static String convertToFileURL(String filename) {
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

            if (nodeStack.isEmpty()) {
                testcases.add(new Testcase(atts.getValue("name"), Nonterminal.valueOf(atts.getValue("type")), atts.getValue("expected")));
            } else {
                throw new IllegalStateException("Testcases cannot be nested.");
            }
        } else {
            Node        node;
            String      content = atts.getValue("content");
            NodeType    nt = NodeType.valueOf(atts.getValue("op"));

            if (content != null) {
                node = new Node(nt, content);
            } else {
                node = new Node(nt);
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
