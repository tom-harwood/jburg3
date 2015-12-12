import jburg.ProductionTable;
import jburg.Reducer;

/**
 * Create a table of arithmetic operations and try some arithmetic.
 */
public class FirstTest
{
    public static void main(String[] args)
    throws Exception
    {
        // The only command line parameter to this routine
        // is the filename of the XML file with testcases;
        // it's called from build.xml so it may be presumed
        // to be a valid file path.
        NodeFactory nf = new NodeFactory(args[0]);

        ProductionTable<Nonterminal, NodeType> productions = new ProductionTable<Nonterminal, NodeType>();

        productions.addPatternMatch(Nonterminal.Int, NodeType.IntLiteral, Node.class.getDeclaredMethod("intLiteral"));

        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, Node.class.getDeclaredMethod("add", Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, Node.class.getDeclaredMethod("identity", Integer.class), Nonterminal.Int);

        productions.addPatternMatch(Nonterminal.Int, NodeType.Multiply, Node.class.getDeclaredMethod("multiply", Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);

        productions.addPatternMatch(Nonterminal.Int, NodeType.Subtract, Node.class.getDeclaredMethod("negate", Integer.class), Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.Subtract, Node.class.getDeclaredMethod("subtract", Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);

        productions.addPatternMatch(Nonterminal.Short, NodeType.ShortLiteral, Node.class.getDeclaredMethod("shortLiteral"));
        productions.addClosure(Nonterminal.Int, Nonterminal.Short, Node.class.getDeclaredMethod("widenShortToInt", Short.class));

        productions.addPatternMatch(Nonterminal.String, NodeType.StringLiteral, Node.class.getDeclaredMethod("stringLiteral"));
        productions.addPatternMatch(Nonterminal.String, NodeType.Add, Node.class.getDeclaredMethod("concat", String.class, String.class), Nonterminal.String, Nonterminal.String);

        productions.generateStates();
        productions.dump(new java.io.PrintWriter("/Users/tharwood/tmp/burmdump.xml"));

        Reducer<Nonterminal, NodeType> reducer = new Reducer<Nonterminal, NodeType>(productions);

        for (Testcase tc: nf.testcases) {
            reducer.label(tc.root);
            String result = reducer.reduce(tc.root, tc.type).toString();;

            if (tc.expected.equals(result)) {
                System.out.printf("Succeeded: %s\n", tc.name);
            } else {
                System.out.printf("FAILED: %s: expected %s got %s\n", tc.name, tc.expected, result);
            }
        }
    }
}
