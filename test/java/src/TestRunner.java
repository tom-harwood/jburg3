import java.util.*;

import jburg.ProductionTable;
import jburg.Reducer;
import jburg.TransitionTableLoader;

/**
 * Run a test as specified by command-line options.
 */
public class TestRunner
{
    public static void main(String[] args)
    throws Exception
    {
        String dumpFile = null;
        String loadFile = null;
        String grammarFile = null;
        String testcaseFile = null;
        String dumpTemplates = "xml.stg";
        String dumpClassName = null;
        String reducerClassName = "Calculator";

        List<String>    failedTestcases = new ArrayList<String>();

        boolean verbose = true;

        GrammarBuilder<Nonterminal,NodeType> grammarBuilder = new GrammarBuilder<Nonterminal,NodeType>(Nonterminal.class, NodeType.class);

        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-load")) {
                loadFile = args[++i];
            } else if (args[i].equals("-classname")) {
                dumpClassName = args[++i];
            } else if (args[i].equals("-dump")) {
                dumpFile = args[++i];
            } else if (args[i].equals("-grammar")) {
                grammarFile = args[++i];
            } else if (args[i].equals("-quiet")) {
                verbose = false;
            } else if (args[i].equals("-randomize")) {
                grammarBuilder.randomizeProductions = true;
            } else if (args[i].equals("-reducer")) {
                reducerClassName = args[++i];
            } else if (args[i].equals("-templates")) {
                dumpTemplates = args[++i];
            } else if (testcaseFile == null) {
                testcaseFile = args[i];
            } else {
                throw new IllegalArgumentException("unrecognized argument " + args[i]);
            }
        }

        ProductionTable<Nonterminal, NodeType> productions = null;

        if (loadFile != null) {
            productions = new TransitionTableLoader<Nonterminal, NodeType>().load(NodeFactory.convertToFileURL(loadFile), Nonterminal.class, NodeType.class);

            if (productions != null) {
                System.out.println("Load successful.");
            } else {
                System.out.printf("Unable to load %s\n", loadFile);
                System.exit(1);
            }

        } else if (grammarFile != null) {
            productions = grammarBuilder.build(NodeFactory.convertToFileURL(grammarFile));
        }

        if (productions == null) {
            throw new IllegalArgumentException("You must specify a grammar, e.g. -grammar burmGrammar.xml, or load a production table, e.g. -load productionTable.xml.\n");
        }

        if (dumpFile != null) {
            productions.dump(dumpFile, dumpClassName, dumpTemplates);
        }

        if (dumpFile == null && testcaseFile != null) {

            Reducer<Nonterminal, NodeType> reducer = new Reducer<Nonterminal, NodeType>(Class.forName(reducerClassName).newInstance(), productions);
            NodeFactory nf = new NodeFactory(testcaseFile);

            for (Testcase tc: nf.testcases) {
                try {
                    reducer.label(tc.root);
                    String result = reducer.reduce(tc.root, tc.type).toString();;

                    if (tc.expected.equals(result)) {
                        if (verbose) {
                            System.out.printf("Succeeded: %s\n", tc.name);
                            System.out.flush();
                        }
                    } else {
                        failedTestcases.add(String.format("FAILED: %s: expected %s got %s", tc.name, tc.expected, result));
                    }
                } catch (Exception ex) {
                    if (tc.expectedException != null && ex.toString().matches(tc.expectedException)) {
                        if (verbose) {
                            System.out.printf("Succeeded: %s negative case caught expected %s\n", tc.name, ex);
                        }
                    } else {
                        failedTestcases.add(String.format("FAILED: %s: unexpected exception %s", tc.name, ex));
                    }
                }
            }
        }

        for (String tcFail: failedTestcases) {
            System.err.println(tcFail);
        }

        System.exit(failedTestcases.size());
    }
}
