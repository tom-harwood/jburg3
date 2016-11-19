package jburg.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jburg.ProductionTable;
import jburg.TransitionTableLoader;
import jburg.frontend.XMLGrammar;

/**
 * GenerateHostBURM is a convenience entry point
 * that uses command-line switches to load a
 * production table, and to dump that table using
 * the specified template group.
 */
public class GenerateHostBURM
{
    /** We don't know the compile-time type of the nonterminal enum, so fake it. */
    enum DummyNonterminal { Fee, Fie };
    /** We don't know the compile-time type of the node type enum, so fake it. */
    enum DummyNodeType    { Fo, Fum };

    public static void main(String[] args)
    throws Exception
    {
        String outputFileName = null;
        String grammarFile = null;
        String templateGroup = null;
        String burmClassName = null;
        String visitorClassName = null;
        String nodeClassName = null;
        String nodeTypeClassAlias = null;

        Map<String,String> attributes = new HashMap<String,String>();

        // TODO: This should be a more general abstraction
        // so non-Java development is less painful!
        Class nonterminalClass = null;
        Class nodeTypeClass = null;

        List<String>    includes = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-classname")) {
                burmClassName = args[++i];
            } else if (args[i].equals("-grammar")) {
                grammarFile = args[++i];
            } else if (args[i].equalsIgnoreCase("-include")) {
                includes.add(args[++i]);
            } else if (args[i].equalsIgnoreCase("-nonterminalClass")) {
                nonterminalClass = Class.forName(args[++i]);
            } else if (args[i].equalsIgnoreCase("-nodeClass")) {
                nodeClassName = args[++i];
            } else if (args[i].equalsIgnoreCase("-nodeTypeClass")) {
                nodeTypeClass = Class.forName(args[++i]);
            } else if (args[i].equalsIgnoreCase("-nodeTypeClassAlias")) {
                nodeTypeClassAlias = args[++i];
            } else if (args[i].equals("-output")) {
                outputFileName = args[++i];
            } else if (args[i].equalsIgnoreCase("-resultType")) {
                attributes.put("result.type", args[++i]);
            } else if (args[i].equals("-visitor")) {
                visitorClassName = args[++i];
            } else if (args[i].equals("-templateGroup")) {
                templateGroup = args[++i];
            } else if (args[i].equals("-visitor")) {
                visitorClassName = args[++i];
            } else {
                throw new IllegalArgumentException("unrecognized argument " + args[i]);
            }
        }

        if (outputFileName == null) {
            throw new IllegalArgumentException("-output must be specified.");

        } else if (grammarFile == null) {
            throw new IllegalArgumentException("-grammar must be specified.");

        } else if (templateGroup == null) {
            throw new IllegalArgumentException("-templateGroup must be specified.");

        } else if (burmClassName == null) {
            throw new IllegalArgumentException("-classname must be specified.");

        } else if (visitorClassName == null) {
            throw new IllegalArgumentException("-visitor must be specified.");

        } else if (nonterminalClass == null) {
            throw new IllegalArgumentException("-nonterminalClass must be specified.");

        } else if (nodeClassName == null) {
            throw new IllegalArgumentException("-nodeClass must be specified.");

        } else if (nodeTypeClass == null) {
            throw new IllegalArgumentException("-nodeTypeClass must be specified.");
        }

        XMLGrammar<?,?> grammarBuilder = new XMLGrammar<DummyNonterminal,DummyNodeType>(nonterminalClass, nodeTypeClass);
        ProductionTable<?, ?> productions = grammarBuilder.build(convertToFileURL(grammarFile));

        attributes.put("class.name", burmClassName);
        attributes.put("visitor.class", visitorClassName);
        attributes.put("node.class", nodeClassName);
        attributes.put("nonterminal.class", nonterminalClass.getName());

        Map<String,Object> defaults = new HashMap<String, Object>();
        defaults.put("includes",includes);

        if (nodeTypeClassAlias == null) {
            attributes.put("nodeType.class", nodeTypeClass.getName());
        } else {
            attributes.put("nodeType.class", nodeTypeClassAlias);
        }

        productions.dump(outputFileName, templateGroup, attributes, defaults);
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
}
