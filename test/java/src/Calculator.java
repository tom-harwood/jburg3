import java.util.*;

import jburg.ProductionTable;
import jburg.Reducer;
import jburg.TransitionTableLoader;

/**
 * Create a table of arithmetic operations and try some arithmetic.
 */
public class Calculator
{
    public static void main(String[] args)
    throws Exception
    {
        String dumpFile = null;
        String loadFile = null;
        String grammarFile = null;
        String testcaseFile = null;

        List<String>    failedTestcases = new ArrayList<String>();

        boolean verbose = true;

        GrammarBuilder<Nonterminal,NodeType> grammarBuilder = new GrammarBuilder<Nonterminal,NodeType>(Nonterminal.class, NodeType.class);

        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-load")) {
                loadFile = args[++i];
            } else if (args[i].equals("-dump")) {
                dumpFile = args[++i];
            } else if (args[i].equals("-grammar")) {
                grammarFile = args[++i];
            } else if (args[i].equals("-quiet")) {
                verbose = false;
            } else if (args[i].equals("-randomize")) {
                grammarBuilder.randomizeProductions = true;
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
            productions.dump(dumpFile);
        }

        if (dumpFile == null && testcaseFile != null) {

            Reducer<Nonterminal, NodeType> reducer = new Reducer<Nonterminal, NodeType>(new Calculator(), productions);
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

    /*
     * ** Callback routines **
     */

    /*
     * ** Nullary Operators **
     */
    public Integer intLiteral(Node node)
    {
        return Integer.valueOf(node.content);
    }

    public Short shortLiteral(Node node)
    {
        return Short.valueOf(node.content);
    }

    public String stringLiteral(Node node)
    {
        return node.content;
    }

    /*
     * ** Unary Operators **
     */
    public Integer negate(Node node, Integer x)
    {
        return -x;
    }

    public Integer identity(Node node, Integer x)
    {
        return x;
    }

    /*
     * ** Binary Operators **
     */
    public Integer add(Node node, Integer x, Integer y)
    {
        return x + y;
    }

    public Integer subtract(Node node, Integer x, Integer y)
    {
        return x - y;
    }

    public Integer multiply(Node node, Integer x, Integer y)
    {
        return x * y;
    }

    public Integer divide(Node node, Integer x, Integer y)
    {
        return x / y;
    }

    public String concat(Node node, String rhs, String lhs)
    {
        return rhs + lhs;
    }

    public String concat(Node node, String... args)
    {
        StringBuilder result = new StringBuilder();

        for (String s: args) {
            result.append(s);
        }

        return result.toString();
    }

    /**
     * ** Ternary operators, for testing **
     */
    public Integer addTernary(Node node, Integer x, Integer y, Integer z)
    {
        return x + y + z;
    }

    /*
     * ** Conversion operators **
     */
    public Integer widenShortToInt(Node node, Short operand)
    {
        return operand.intValue();
    }

    public String convertToString(Node node, Object o)
    {
        return o.toString();
    }

    /*
     * ** N-ary operators
     */
    public Integer addNary(Node node, Integer... args)
    {
        int result = 0;
        for (Integer i: args) {
            result += i;
        }
        return result;
    }

    /*
     * Predicate methods
     */
    public Boolean shortGuard(Node n)
    {
        int nodeValue = Integer.valueOf(n.content);
        return nodeValue >= Short.MIN_VALUE && nodeValue <= Short.MAX_VALUE;
    }
}
