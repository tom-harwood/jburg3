package tl3;

import java.util.List;
import java.util.Stack;
import org.antlr.v4.runtime.tree.TerminalNode;

import static tl3.NodeType.*;

class NodeBuilder extends tl3BaseListener
{
    private Stack<Node> nodeStack = new Stack<Node>();

    Node getResult()
    {
        Node result = nodeStack.pop();
        assert nodeStack.isEmpty();
        return result;
    }

	@Override
    public void exitScope(tl3Parser.ScopeContext ctx)
    {
        Node result = new Node(Scope, 2);

        result.setChild(0, nodeStack.pop());

        if (ctx.ID() != null) {
            result.setChild(1, new Node(IdentifierPart, ctx.ID().getSymbol().getText()));
        }

        nodeStack.push(result);
    }

	@Override
    public void exitScopeContents(tl3Parser.ScopeContentsContext ctx)
    {
        Node result = new Node(ScopeContents, ctx.getChildCount());
        // Nodes are pushed in reverse order.
        for (int i = ctx.getChildCount() - 1; i >= 0; i--) {
            result.setChild(i, nodeStack.pop());
        }
        nodeStack.push(result);
    }

	@Override
    public void exitAssignment(tl3Parser.AssignmentContext ctx)
    {
        Node result = new Node(Assignment, 2);
        result.setChild(1, nodeStack.pop());
        result.setChild(0, nodeStack.pop());
        nodeStack.push(result);
    }

	@Override
    public void exitPrint(tl3Parser.PrintContext ctx)
    {
        Node result = new Node(Print, 1);
        result.setChild(0, nodeStack.pop());
        nodeStack.push(result);
    }

	@Override
    public void exitVarDef(tl3Parser.VarDefContext ctx)
    {
        Node result;

        if (ctx.expression() != null) {
            result = new Node(VarDef, 2);
            result.setChild(1, nodeStack.pop());
        } else {
            result = new Node(VarDef, 1);
        }

        result.setChild(0, new Node(IdentifierPart, ctx.ID().getSymbol().getText()));

        nodeStack.push(result);
    }

	@Override
    public void exitArithmeticExpression(tl3Parser.ArithmeticExpressionContext ctx)
    {
        if (ctx.getChildCount() > 1) {
            // TODO: Differentiate based on operator
            Node result = new Node(Add, 2);
            result.setChild(1, nodeStack.pop());
            result.setChild(0, nodeStack.pop());
            nodeStack.push(result);
        } // else let the result flow through
    }

	@Override
    public void exitBooleanExpression(tl3Parser.BooleanExpressionContext ctx)
    {
        if (ctx.getChildCount() > 1) {
            // TODO: Differentiate based on operator
            Node result = new Node(Equal, 2);
            result.setChild(1, nodeStack.pop());
            result.setChild(0, nodeStack.pop());
            nodeStack.push(result);
        } // else let the result flow through
    }

	@Override
    public void exitPrimaryExpression(tl3Parser.PrimaryExpressionContext ctx)
    {
        Node result;

        if (ctx.INT() != null) {
            result = new Node(IntegerLiteral, Integer.valueOf(ctx.INT().getSymbol().getText()));
        } else if (ctx.StringLiteral() != null) {
            result = new Node(StringLiteral, ctx.StringLiteral().getSymbol().getText());
        } else {
            result = nodeStack.pop();
        }

        nodeStack.push(result);
    }

	@Override
    public void exitIdentifier(tl3Parser.IdentifierContext ctx)
    {
        List<TerminalNode> nameElements = ctx.ID();
        Node result = new Node(Identifier, nameElements.size());

        int i = 0;
        for (TerminalNode name: nameElements) {
            result.setChild(i++, new Node(IdentifierPart, name.getSymbol().getText()));
        }

        nodeStack.push(result);
    }

    public void exitBuiltinProcedure(tl3Parser.BuiltinProcedureContext ctx)
    {
        Node result = new Node(Verify, 3);
        result.setChild(2, nodeStack.pop());
        result.setChild(1, nodeStack.pop());
        result.setChild(0, nodeStack.pop());

        nodeStack.push(result);
    }
}
