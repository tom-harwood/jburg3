
import jburg.ProductionTable;
import jburg.Reducer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FirstTest
{
    public enum Nonterminal { Int, Short, String };
    public enum NodeType { ShortLiteral, IntLiteral, StringLiteral, Add, Subtract, Multiply };

    public static class Node implements jburg.BurgInput<NodeType>
    {
        NodeType    nodeType;
        List<Node>  children;

        Node(NodeType type)
        {
            this.nodeType = type;
            this.children = Collections.emptyList();
        }

        Node(NodeType type, Node... children)
        {
            this.nodeType = type;
            this.children = Arrays.asList(children);
        }

        public NodeType getNodeType()
        {
            return this.nodeType;
        }

        public int getSubtreeCount()
        {
            return children.size();
        }

        public Node getSubtree(int idx)
        {
            return children.get(idx);
        }

        public void setStateNumber(int stateNumber)
        {
            this.stateNumber = stateNumber;
        }

        public int getStateNumber()
        {
            return this.stateNumber;
        }

        private int stateNumber = -1;

        Object payload;

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

        public String stringLiteral()
        {
            return payload.toString();
        }

        public String concat(String lhs, String rhs)
        {
            return lhs + rhs;
        }
    }

    public static void main(String[] args)
    throws Exception
    {
        ProductionTable<Nonterminal, NodeType> productions = new ProductionTable<Nonterminal, NodeType>();

        productions.addPatternMatch(Nonterminal.Int, NodeType.IntLiteral, Node.class.getDeclaredMethod("intLiteral"));

        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, Node.class.getDeclaredMethod("add", Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.Add, Node.class.getDeclaredMethod("identity", Integer.class), Nonterminal.Int);

        productions.addPatternMatch(Nonterminal.Int, NodeType.Multiply, Node.class.getDeclaredMethod("multiply", Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);

        productions.addPatternMatch(Nonterminal.Int, NodeType.Subtract, Node.class.getDeclaredMethod("negate", Integer.class), Nonterminal.Int);
        productions.addPatternMatch(Nonterminal.Int, NodeType.Subtract, Node.class.getDeclaredMethod("subtract", Integer.class, Integer.class), Nonterminal.Int, Nonterminal.Int);

        productions.addPatternMatch(Nonterminal.Short, NodeType.ShortLiteral, Node.class.getDeclaredMethod("shortLiteral"));
        productions.addClosure(Nonterminal.Int, Nonterminal.Short);

        productions.addPatternMatch(Nonterminal.String, NodeType.StringLiteral, Node.class.getDeclaredMethod("stringLiteral"));
        productions.addPatternMatch(Nonterminal.String, NodeType.Add, Node.class.getDeclaredMethod("concat", String.class, String.class), Nonterminal.String, Nonterminal.String);

        productions.generateStates();
        productions.dump(new java.io.PrintWriter(System.out));

        Node lhs  = new Node(NodeType.IntLiteral);
        lhs.payload = Integer.valueOf(1);
        Node rhs  = new Node(NodeType.IntLiteral);
        rhs.payload = Integer.valueOf(2);
        Node root = new Node(NodeType.Add, lhs, rhs);

        Reducer<Nonterminal, NodeType> reducer = new Reducer<Nonterminal, NodeType>(productions);
        reducer.label(root);
        Integer result = (Integer)reducer.reduce(root, Nonterminal.Int);

        if (result == 3) {
            System.out.printf("Succeeded: %s == 3\n", result);
        } else {
            System.out.printf("FAILED: %s != 3\n", result);
            System.exit(1);
        }
    }

}
