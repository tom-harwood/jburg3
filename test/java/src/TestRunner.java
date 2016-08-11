import java.util.*;
import java.lang.reflect.Method;

import jburg.ProductionTable;
import jburg.Reducer;
import jburg.TransitionTableLoader;
import jburg.frontend.XMLGrammar;

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
        String visitorClassName = "Calculator";
        String reducerClassName = null;

        List<String>    failedTestcases = new ArrayList<String>();

        boolean verbose = true;

        XMLGrammar<Nonterminal,NodeType> grammarBuilder = new XMLGrammar<Nonterminal,NodeType>(Nonterminal.class, NodeType.class);

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
                grammarBuilder.setRandomized(true);
            } else if (args[i].equals("-reducer")) {
                reducerClassName = args[++i];
            } else if (args[i].equals("-visitor")) {
                visitorClassName = args[++i];
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

        if (productions == null && reducerClassName ==  null) {
            throw new IllegalArgumentException("You must specify a grammar, e.g. -grammar burmGrammar.xml, or load a production table, e.g. -load productionTable.xml.\n");
        }

        if (dumpFile != null) {
            Map<String,String> attributes = new HashMap<String,String>();
            attributes.put("class.name", dumpClassName);
            attributes.put("visitor.class", visitorClassName);
            attributes.put("node.class", "Node");
            attributes.put("nonterminal.class", "Nonterminal");
            attributes.put("nodeType.class", "NodeType");
            productions.dump(dumpFile, dumpTemplates, attributes);
        }

        if (dumpFile == null && testcaseFile != null) {

            Reducer<Nonterminal, NodeType>  defaultReducer = null;
            Object                          bespokeReducer = null;
            Method                          labelMethod = null;
            Method                          reduceMethod = null;
            Object                          visitor = Class.forName(visitorClassName).newInstance();

            if (reducerClassName == null) {
                defaultReducer = new Reducer<Nonterminal, NodeType>(visitor, productions);
            } else {
                bespokeReducer = Class.forName(reducerClassName).newInstance();
                labelMethod = bespokeReducer.getClass().getDeclaredMethod("label", visitor.getClass(), Node.class);
                reduceMethod = bespokeReducer.getClass().getDeclaredMethod("reduce", visitor.getClass(), Node.class, Nonterminal.class);
            }

            NodeFactory nf = new NodeFactory(testcaseFile);

            for (Testcase tc: nf.testcases) {
                try {
                    String result = null;

                    if (bespokeReducer == null) {
                        defaultReducer.label(tc.root);
                        result = defaultReducer.reduce(tc.root, tc.type).toString();;
                    } else {
                        labelMethod.invoke(bespokeReducer, visitor, tc.root);
                        Object reduced = reduceMethod.invoke(bespokeReducer, visitor, tc.root, tc.type);

                        if (reduced != null) {
                            result = reduced.toString();
                        }
                    }

                    if (tc.expected.equals(result)) {
                        if (verbose) {
                            System.out.printf("Succeeded: %s\n", tc.name);
                        }
                    } else {
                        failedTestcases.add(String.format("FAILED: %s: expected %s got %s", tc.name, tc.expected, result));
                    }
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    Throwable ex = ite.getCause();
                    if (tc.expectedException != null && ex.toString().matches(tc.expectedException)) {
                        if (verbose) {
                            System.out.printf("Succeeded: %s negative case caught expected %s\n", tc.name, ex);
                        }
                    } else {
                        failedTestcases.add(String.format("FAILED: %s: unexpected exception %s", tc.name, ex));
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

        if (!failedTestcases.isEmpty()) {
            System.out.println();
            System.out.println("---FAILURES---");
            System.out.flush();
            Thread.sleep(100);
        }

        for (String tcFail: failedTestcases) {
            System.err.println(tcFail);
        }

        System.exit(failedTestcases.size());
    }
}
