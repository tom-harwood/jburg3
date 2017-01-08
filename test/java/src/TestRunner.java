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
        String verboseTrigger = null;

        List<String>    failedTestcases = new ArrayList<String>();

        boolean verbose = true;
        boolean randomize = false;


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
            } else if (args[i].equals("-verbose-trigger")) {
                verboseTrigger = args[++i];
            } else if (args[i].equals("-randomize")) {
                randomize = true;
            } else if (args[i].equals("-reducer")) {
                reducerClassName = args[++i];
            } else if (args[i].equals("-visitor")) {
                visitorClassName = args[++i];
            } else if (args[i].equals("-templates")) {
                dumpTemplates = args[++i];
            } else if (testcaseFile == null && !args[i].startsWith("-")) {
                testcaseFile = args[i];
            } else {
                throw new IllegalArgumentException("unrecognized argument " + args[i]);
            }
        }

        XMLGrammar<Nonterminal,NodeType> grammarBuilder = new XMLGrammar<Nonterminal,NodeType>("Nonterminal", "NodeType");
        grammarBuilder.setRandomized(randomize);

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

        if (verboseTrigger != null) {
            productions.setVerboseTrigger(verboseTrigger);
        }

        if (dumpFile != null) {
            Map<String,String> attributes = new HashMap<String,String>();
            attributes.put("class.name", dumpClassName);
            attributes.put("visitor.class", visitorClassName);
            attributes.put("node.class", "Node");
            attributes.put("nonterminal.class", "Nonterminal");
            attributes.put("nodeType.class", "NodeType");

            if (grammarFile != null) {
                attributes.put("grammar.name", grammarFile);
            }

            Map<String,Object> defaults = new HashMap<String, Object>();
            productions.dump(dumpFile, dumpTemplates, attributes, defaults);
        }

        if (dumpFile == null && testcaseFile != null) {

            Reducer<Nonterminal, NodeType>  defaultReducer = null;
            Object                          bespokeReducer = null;
            Method                          labelMethod = null;
            Method                          reduceMethod = null;
            Method                          canProduceMethod = null;
            Object                          visitor = Class.forName(visitorClassName).newInstance();

            if (reducerClassName == null) {
                defaultReducer = new Reducer<Nonterminal, NodeType>(visitor, productions);
            } else {
                bespokeReducer = Class.forName(reducerClassName).newInstance();
                labelMethod = bespokeReducer.getClass().getDeclaredMethod("label", visitor.getClass(), Node.class);
                reduceMethod = bespokeReducer.getClass().getDeclaredMethod("reduce", visitor.getClass(), Node.class, Nonterminal.class);
                canProduceMethod = bespokeReducer.getClass().getDeclaredMethod("canProduce", Node.class, Nonterminal.class);
            }

            NodeFactory nf = new NodeFactory(testcaseFile);

            for (Testcase tc: nf.testcases) {

                if (bespokeReducer != null) {
                    labelMethod.invoke(bespokeReducer, visitor, tc.root);
                } else {
                    defaultReducer.label(tc.root);
                }

                switch(tc.testType) {
                    case CanProduce:
                    case CannotProduce: {
                        boolean canProduce;

                        if (bespokeReducer != null) {
                            canProduce = (Boolean)canProduceMethod.invoke(bespokeReducer, tc.root, tc.canProduceType);
                        } else {
                            canProduce = defaultReducer.canProduce(tc.root, tc.canProduceType);
                        }

                        if (canProduce && tc.testType == Testcase.TestType.CanProduce) {
                            if (verbose) {
                                System.out.printf("Succeeded: %s can produce %s\n", tc.name, tc.canProduceType);
                            }
                        } else if (!canProduce) {
                            if (verbose) {
                                System.out.printf("Succeeded: %s cannot produce %s\n", tc.name, tc.canProduceType);
                            }
                        } else {
                            failedTestcases.add(String.format("FAILED: %s: canProduce %s expected %s got %s", tc.name, tc.canProduceType, tc.canProduceType, canProduce));
                        }
                        break;
                    }

                case Normal:
                    try {
                        String result = null;

                        if (bespokeReducer == null) {
                            result = defaultReducer.reduce(tc.root, tc.type).toString();
                        } else {
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

                        if (verbose) {
                            ex.printStackTrace();
                        }

                        failedTestcases.add(String.format("FAILED: %s: unexpected exception %s", tc.name, ex));
                    } catch (Exception ex) {

                        if (verbose) {
                            ex.printStackTrace();
                        }
                        failedTestcases.add(String.format("FAILED: %s: unexpected exception %s", tc.name, ex));
                    }
                    break;
                case Negative: {
                    Throwable ex = null;

                    try {
                        if (bespokeReducer == null) {
                            defaultReducer.reduce(tc.root, tc.type);
                        } else {
                            reduceMethod.invoke(bespokeReducer, visitor, tc.root, tc.type);
                        }

                        failedTestcases.add(String.format("FAILED: %s: expected exception %s", tc.name, tc.expected));
                        break;

                    } catch (java.lang.reflect.InvocationTargetException ite) {
                        ex = (Exception)ite.getCause();
                    } catch (Exception exOrig) {
                        ex = exOrig;
                    }

                    if (ex.toString().matches(tc.expected)) {

                        if (verbose) {
                            System.out.printf("Succeeded: %s negative case caught expected %s\n", tc.name, ex);
                        }
                    } else {

                        if (verbose) {
                            ex.printStackTrace();
                        }

                        failedTestcases.add(String.format("FAILED: %s: expected exception %s, got %s", tc.name, tc.expected, ex));
                    }
                    break;
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
