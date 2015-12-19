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

        // Leaf operators
        productions.addPatternMatch(Nonterminal.Int, NodeType.IntLiteral, FirstTest.class.getDeclaredMethod("intLiteral", Node.class));
        productions.addPatternMatch(Nonterminal.String, NodeType.StringLiteral, FirstTest.class.getDeclaredMethod("stringLiteral", Node.class));
        productions.addPatternMatch(Nonterminal.Short, NodeType.ShortLiteral, FirstTest.class.getDeclaredMethod("shortLiteral", Node.class));

        // Unary operators
        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, FirstTest.class.getDeclaredMethod("identity", Node.class, Integer.class), Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.Subtract, FirstTest.class.getDeclaredMethod("negate", Node.class, Integer.class), Nonterminal.Int);

        // Binary operators
        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, FirstTest.class.getDeclaredMethod("add", Node.class, Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.Multiply, FirstTest.class.getDeclaredMethod("multiply", Node.class, Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.Subtract, FirstTest.class.getDeclaredMethod("subtract", Node.class, Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.String, NodeType.Add, FirstTest.class.getDeclaredMethod("concat", Node.class, String.class, String.class), Nonterminal.String, Nonterminal.String);

        // Ternary operators
        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, FirstTest.class.getDeclaredMethod("addTernary", Node.class, Integer.class,Integer.class,Integer.class), Nonterminal.Int, Nonterminal.Int, Nonterminal.Int);

        // Conversion operators
        productions.addClosure(Nonterminal.Int, Nonterminal.Short, FirstTest.class.getDeclaredMethod("widenShortToInt", Node.class, Short.class));

        productions.generateStates();
        productions.dump(new java.io.PrintWriter("/Users/tharwood/tmp/burmdump.xml"));

        Reducer<Nonterminal, NodeType> reducer = new Reducer<Nonterminal, NodeType>(new FirstTest(), productions);

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

    public String concat(Node node, String lhs, String rhs)
    {
        return lhs + rhs;
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

}
