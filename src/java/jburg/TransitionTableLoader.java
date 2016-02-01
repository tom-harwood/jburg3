package jburg;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class TransitionTableLoader<Nonterminal, NodeType> extends DefaultHandler
{
    ProductionTable<Nonterminal, NodeType> table = null;

    Stack<HyperPlane<Nonterminal, NodeType>> hyperplanes = new Stack<HyperPlane<Nonterminal, NodeType>>();

    public ProductionTable<Nonterminal, NodeType> load(String uri)
    throws Exception
    {

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(this);

        this.table = new ProductionTable<Nonterminal, NodeType>();

        try {
            xmlReader.parse(uri);

        } catch (Exception ex) {
            this.table = null;
            throw ex;
        }

        return this.table;
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException
    {
        /*
         * State table
         */
        if (localName.equals("state")) {

        /*
         * Pattern matching
         */
        } else if (localName.equals("patterns")) {
        } else if (localName.equals("pattern")) {
        } else if (localName.equals("childTypes")) {
        } else if (localName.equals("childType")) {

        /*
         * Method decoding
         */
        } else if (localName.equals("preCallback")) {
        } else if (localName.equals("postCallback")) {
        } else if (localName.equals("method")) {
        } else if (localName.equals("parameter")) {

        /*
         * Closures
         */
        } else if (localName.equals("closure")) {

        /*
         * Transition table
         */
        } else if (localName.equals("operator")) {
        } else if (localName.equals("plane")) {
        } else if (localName.equals("mappedState")) {
        } else if (localName.equals("finalDimension")) {
        } else if (localName.equals("entry")) {
        } else if (localName.equals("predicatedState")) {
        } else if (localName.equals("predicated")) {
        } else if (localName.equals("default")) {
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
    {
    }
}
