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
    public static void main(String[] args)
    throws Exception
    {
        String outputFileName = null;
        String grammarFileName = null;
        String templateGroup = null;
        String burmClassName = null;
        String visitorClassName = null;
        String nodeClassName = null;
        String nodeTypeClassAlias = null;

        String verboseTrigger = null;

        Map<String,String> attributes = new HashMap<String,String>();

        String nonterminalClassName = null;
        String nodeTypeClassName = null;

        List<String>    includes = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-classname")) {
                burmClassName = args[++i];
            } else if (args[i].equals("-grammar")) {
                grammarFileName = args[++i];
            } else if (args[i].equalsIgnoreCase("-include")) {
                includes.add(args[++i]);
            } else if (args[i].equalsIgnoreCase("-nonterminalClass")) {
                nonterminalClassName = args[++i];
            } else if (args[i].equalsIgnoreCase("-nodeClass")) {
                nodeClassName = args[++i];
            } else if (args[i].equalsIgnoreCase("-nodeTypeClass")) {
                nodeTypeClassName = args[++i];
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
            } else if (args[i].equals("-verbose")) {
                verboseTrigger = args[++i];
            } else {
                throw new IllegalArgumentException("unrecognized argument " + args[i]);
            }
        }

        if (outputFileName == null) {
            throw new IllegalArgumentException("-output must be specified.");

        } else if (grammarFileName == null) {
            throw new IllegalArgumentException("-grammar must be specified.");

        } else if (templateGroup == null) {
            throw new IllegalArgumentException("-templateGroup must be specified.");

        } else if (burmClassName == null) {
            throw new IllegalArgumentException("-classname must be specified.");

        } else if (visitorClassName == null) {
            throw new IllegalArgumentException("-visitor must be specified.");

        } else if (nonterminalClassName == null) {
            throw new IllegalArgumentException("-nonterminalClass must be specified.");

        } else if (nodeClassName == null) {
            throw new IllegalArgumentException("-nodeClass must be specified.");

        } else if (nodeTypeClassName == null) {
            throw new IllegalArgumentException("-nodeTypeClass must be specified.");
        }

        XMLGrammar<String,String> grammarBuilder = new XMLGrammar<String,String>(nonterminalClassName, nodeTypeClassName);
        grammarBuilder.setVerboseTrigger(verboseTrigger);
        ProductionTable<String, String> productions = grammarBuilder.build(convertToFileURL(grammarFileName));

        attributes.put("class.name", burmClassName);
        attributes.put("visitor.class", visitorClassName);
        attributes.put("node.class", nodeClassName);
        attributes.put("nonterminal.class", nonterminalClassName);
        attributes.put("grammar.name", grammarFileName);

        Map<String,Object> defaults = new HashMap<String, Object>();
        defaults.put("includes",includes);

        if (nodeTypeClassAlias == null) {
            attributes.put("nodeType.class", nodeTypeClassName);
        } else {
            attributes.put("nodeType.class", nodeTypeClassAlias);
        }
        attributes.put("nodeClass", nodeClassName);

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
