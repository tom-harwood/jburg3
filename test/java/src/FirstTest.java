
import jburg.ProductionTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FirstTest
{
    public enum Nonterminal { Int, Short };
    public enum NodeType { ShortLiteral, IntLiteral, Add, Subtract, Multiply };

    public static class Node
    {
        NodeType    type;
        List<Node>  children;

        Node(NodeType type)
        {
            this.type = type;
            this.children = Collections.emptyList();
        }

        Node(NodeType type, Node... children)
        {
            this.type = type;
            this.children = Arrays.asList(children);
        }

        int     stateNumber = -1;

        Object  payload;

        /*
         * ** Nullary Operators **
         */
        public Integer intLiteral()
        {
            return (Integer)payload;
        }

        public Integer shortLiteral()
        {
            return intLiteral();
        }

        /*
         * ** Unary Operators **
         */
        public Integer negate(Integer x)
        {
            return -x;
        }

        public Integer identity(Integer x)
        {
            return x;
        }

        /*
         * ** Binary Operators **
         */
        public Integer add(Integer x, Integer y)
        {
            return x + y;
        }

        public Integer subtract(Integer x, Integer y)
        {
            return x - y;
        }

        public Integer multiply(Integer x, Integer y)
        {
            return x * y;
        }

        public Integer divide(Integer x, Integer y)
        {
            return x / y;
        }
    }

    public static void main(String[] args)
    throws Exception
    {
        ProductionTable<Nonterminal, NodeType> productions = new ProductionTable<Nonterminal, NodeType>();

        productions.addPatternMatch(Nonterminal.Int, NodeType.IntLiteral, Node.class.getDeclaredMethod("intLiteral"));

        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, Node.class.getDeclaredMethod("add", Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, Node.class.getDeclaredMethod("identity", Integer.class), Nonterminal.Int);
        //productions.addPatternMatch(Nonterminal.Int, NodeType.Multiply, Node.class.getDeclaredMethod("multiply", Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);
        //productions.addPatternMatch(Nonterminal.Int, NodeType.Subtract, Node.class.getDeclaredMethod("negate", Integer.class), Nonterminal.Int);
        //productions.addPatternMatch(Nonterminal.Int, NodeType.Subtract, Node.class.getDeclaredMethod("subtract", Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);

        productions.addPatternMatch(Nonterminal.Short, NodeType.ShortLiteral, Node.class.getDeclaredMethod("shortLiteral"));
        productions.addClosure(Nonterminal.Int, Nonterminal.Short);

        productions.generateStates();
        productions.dump(new java.io.PrintWriter(System.out));
    }

}
