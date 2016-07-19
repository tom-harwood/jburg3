package jburg.util;

import java.lang.reflect.Method;
import java.util.*;
import java.io.*;

public class GenerateNodeBuilder
{
    public static void main(String[] args)
    throws Exception
    {
        Class<?> antlrVisitor = Class.forName(args[0]);

        PrintWriter out = new PrintWriter(new FileWriter(args[3]));

        List<Method> entryMethods = new ArrayList<Method>();
        List<Method> exitMethods = new ArrayList<Method>();

        for (Method m: antlrVisitor.getDeclaredMethods()) {
            String name = m.getName();
            if (name.indexOf("enter") == 0) {
                entryMethods.add(m);
            } else if (name.indexOf("exit") == 0) {
                exitMethods.add(m);
            } else {
                System.err.printf("Unable to classify method %s\n", m);
            }
        }

        out.printf("package %s;\n\n", args[1]);
        out.println("import java.util.ArrayList;");
        out.println("import java.util.Collections;");
        out.println("import java.util.List;");
        out.println("import java.util.Stack;");
        // FIXME: Parameterize
        out.println("import tql.Node;");
        out.println("import org.antlr.v4.runtime.ParserRuleContext;");
        out.println("import org.antlr.v4.runtime.tree.ErrorNode;");
        out.println("import org.antlr.v4.runtime.tree.TerminalNode;");
        out.println();
        // FIXME: Parameterize so this is not necessary
        out.println("@SuppressWarnings(\"unchecked\")");
        out.printf("\npublic class %s<NodeType> implements %s\n\n{\n", args[2], antlrVisitor.getName());
        out.println("    protected Stack<Node> nodeStack  = new Stack<Node>();");
        out.println("    protected Stack<Integer>   scopeStack = new Stack<Integer>();");
        out.println();

        for (Method entry: entryMethods) {
            out.printf("    @Override public void %s { scopeStack.push(nodeStack.size()); }\n", edit(entry));
        }

        for (Method exit: exitMethods) {
            out.printf("    @Override public void %s { scopeStack.pop(); }\n", edit(exit));
        }

        out.println("    @Override public void enterEveryRule(ParserRuleContext ctx) { }\n");
        out.println("    @Override public void exitEveryRule(ParserRuleContext ctx) { }\n");
        out.println("    @Override public void visitTerminal(TerminalNode node) { }\n");
        out.println("    @Override public void visitErrorNode(ErrorNode node) { }\n");
        out.println();
        out.println("    protected int pendingNodeCount()");
        out.println("    {");
        out.println("        assert !scopeStack.empty();");
        out.println("        return nodeStack.size() - scopeStack.peek();");
        out.println("    }");
        out.println();
        out.println("    protected Node createNode(NodeType type, Object content)");
        out.println("    {");
        out.println("        int childCount = pendingNodeCount();");
        out.println("        assert childCount >= 0: String.format(\"Negative child count: %d\", childCount);");
        out.println("        List<Node> children = new ArrayList<Node>();");
        out.println("        for (int i = 0; i < childCount; i++) {");
        out.println("            children.add(nodeStack.pop());");
        out.println("        }");
        out.println("        Collections.reverse(children);");
        out.println("        Node result = new Node(type, children, content);");
        out.println("        nodeStack.push(result);");
        out.println("        scopeStack.pop();");
        out.println("        return result;");
        out.println("    }");
        out.println();
        out.println("    protected Node createNode(NodeType type)");
        out.println("    {");
        out.println("        return createNode(type, null);");
        out.println("    }");
        out.println();
        out.println("    protected Node getRoot()");
        out.println("    {");
        out.println("        assert nodeStack.size() == 1: String.format(\"Expected 1 child found %d\", nodeStack.size());");
        out.println("        return nodeStack.peek();");
        out.println("    }");
        out.println();
        out.println();
        out.println("}");
        out.flush();
        out.close();
    }

    private static String edit(Method m)
    {
        StringBuilder result = new StringBuilder(m.getName());
        result.append("(");
        Class<?>[] parameterTypes = m.getParameterTypes();

        for (int i = 0; i < parameterTypes.length; i++) {

            if (i > 0) {
                result.append(", ");
            }

            result.append(parameterTypes[i].getName().replaceAll("\\$","."));
            result.append(String.format(" a%d", i));
        }

        result.append(")");

        return result.toString();
    }
}
