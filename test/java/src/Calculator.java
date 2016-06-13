import java.util.*;

import jburg.ProductionTable;
import jburg.Reducer;
import jburg.TransitionTableLoader;

/**
 * Callback and predicate routines for arithmetic tests.
 */
public class Calculator
{
    /*
     * ** Nullary Operators **
     */
    public Integer intLiteral(Node node)
    {
        return node.intValue();
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

    public Integer addWithBias(Node node, Integer x, Integer y)
    {
        int result = x + y + this.additionBias;
        this.additionBias--;
        return result;
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

    public String concatFixed(Node node, String rhs, String lhs)
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

    public Integer addFixedAndVariadic(Node node, Integer fixedArg, Integer... tail)
    {
        int result = fixedArg;

        for (Integer i: tail) {
            result += i;
        }

        return result;
    }

    /*
     * Predicate methods
     */
    public Boolean shortGuard(Node node)
    {
        return node.intValue() >= Short.MIN_VALUE && node.intValue() <= Short.MAX_VALUE;
    }

    /*
     * Pre-reduction methods
     */
    public void biasPreCallback(Node n, Nonterminal goalState)
    {
        additionBias += 1;
    }

    int additionBias = 0;
}
