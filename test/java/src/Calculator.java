import jburg.ProductionTable;
import jburg.Reducer;

/**
 * Create a table of arithmetic operations and try some arithmetic.
 */
public class Calculator
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
        productions.addPatternMatch(Nonterminal.Int, NodeType.IntLiteral, Calculator.class.getDeclaredMethod("intLiteral", Node.class));
        productions.addPatternMatch(Nonterminal.String, NodeType.StringLiteral, Calculator.class.getDeclaredMethod("stringLiteral", Node.class));

        // Predicated leaf operators
        //productions.addPatternMatch(Nonterminal.Short, NodeType.ShortLiteral, Calculator.class.getDeclaredMethod("shortLiteral", Node.class));
        productions.addPatternMatch(Nonterminal.Short, NodeType.ShortLiteral, 1, Calculator.class.getDeclaredMethod("shortGuard", Node.class), null, Calculator.class.getDeclaredMethod("shortLiteral", Node.class), false);

        // Unary operators
        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, Calculator.class.getDeclaredMethod("identity", Node.class, Integer.class), Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.Subtract, Calculator.class.getDeclaredMethod("negate", Node.class, Integer.class), Nonterminal.Int);

        // Binary operators
        // Note: this Add operator is intentionally poorly overloaded to test the production table.
        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, Calculator.class.getDeclaredMethod("add", Node.class, Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.AddStrict, Calculator.class.getDeclaredMethod("add", Node.class, Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.Multiply, Calculator.class.getDeclaredMethod("multiply", Node.class, Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.Subtract, Calculator.class.getDeclaredMethod("subtract", Node.class, Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.String, NodeType.Add, Calculator.class.getDeclaredMethod("concat", Node.class, args.getClass()), Nonterminal.String, Nonterminal.String);

        // Ternary operators
        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, Calculator.class.getDeclaredMethod("addTernary", Node.class, Integer.class,Integer.class,Integer.class), Nonterminal.Int, Nonterminal.Int, Nonterminal.Int);

        // Variadic operators
        productions.addVarArgsPatternMatch(Nonterminal.String, NodeType.Concat, 1, null, Calculator.class.getDeclaredMethod("concat", Node.class, args.getClass()), Nonterminal.String);

        // Conversion operators
        productions.addClosure(Nonterminal.Int, Nonterminal.Short, Calculator.class.getDeclaredMethod("widenShortToInt", Node.class, Short.class));
        productions.addClosure(Nonterminal.String, Nonterminal.Int, Calculator.class.getDeclaredMethod("convertToString", Node.class, Object.class));

        productions.generateStates();
        productions.dump(System.getenv("BURMDUMP"));

        Reducer<Nonterminal, NodeType> reducer = new Reducer<Nonterminal, NodeType>(new Calculator(), productions);

        for (Testcase tc: nf.testcases) {
            try {
                reducer.label(tc.root);
                String result = reducer.reduce(tc.root, tc.type).toString();;

                if (tc.expected.equals(result)) {
                    System.out.printf("Succeeded: %s\n", tc.name);
                } else {
                    System.out.printf("FAILED: %s: expected %s got %s\n", tc.name, tc.expected, result);
                }
            } catch (Exception ex) {
                if (tc.expectedException != null && ex.toString().matches(tc.expectedException)) {
                    System.out.printf("Succeeded: %s negative case caught expected %s\n", tc.name, ex);
                } else {
                    throw ex;
                }
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
